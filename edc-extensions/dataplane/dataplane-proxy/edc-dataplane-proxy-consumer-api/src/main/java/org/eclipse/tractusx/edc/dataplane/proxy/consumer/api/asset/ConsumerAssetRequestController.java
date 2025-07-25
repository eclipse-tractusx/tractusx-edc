/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure;
import org.eclipse.edc.connector.dataplane.spi.pipeline.TransferService;
import org.eclipse.edc.connector.dataplane.util.sink.AsyncStreamingDataSink;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset.model.AssetRequest;
import org.eclipse.tractusx.edc.edr.spi.service.EdrService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.BAD_GATEWAY;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static jakarta.ws.rs.core.Response.status;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.eclipse.edc.connector.dataplane.spi.schema.DataFlowRequestSchema.PATH;
import static org.eclipse.edc.connector.dataplane.spi.schema.DataFlowRequestSchema.QUERY_PARAMS;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.types.RefreshMode.AUTO_REFRESH;

/**
 * Implements the HTTP proxy API.
 */
@Path("/aas")
@Produces(MediaType.APPLICATION_JSON)
public class ConsumerAssetRequestController implements ConsumerAssetRequestApi {
    public static final String BASE_URL = EDC_NAMESPACE + "endpoint";
    public static final String EDR_ERROR_MESSAGE = "No EDR for transfer process: %s : %s";
    private static final String ASYNC_TYPE = "async";
    private static final String HEADER_AUTHORIZATION = "header:authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private final EdrService edrService;
    private final TransferService transferService;
    private final Monitor monitor;

    private final ExecutorService executorService;


    private final Map<String, Function<AssetRequest, String>> mappers = Map.of(
            "providerId", AssetRequest::getProviderId,
            "transferProcessId", AssetRequest::getTransferProcessId,
            "assetId", AssetRequest::getAssetId);

    public ConsumerAssetRequestController(EdrService edrService,
                                          TransferService transferService,
                                          ExecutorService executorService,
                                          Monitor monitor) {
        this.edrService = edrService;
        this.transferService = transferService;
        this.executorService = executorService;
        this.monitor = monitor;
    }

    @POST
    @Path("/request")
    @Override
    public void requestAsset(AssetRequest request, @Suspended AsyncResponse response) {
        // resolve the EDR and add it to the request
        var edr = resolveEdr(request);

        var sourceAddress = dataPlaneAddress(edr);
        var properties = dataPlaneProperties(request);

        var destinationAddress = DataAddress.Builder.newInstance()
                .type(ASYNC_TYPE)
                .build();


        var flowRequest = DataFlowStartMessage.Builder.newInstance()
                .processId(randomUUID().toString())
                .sourceDataAddress(sourceAddress)
                .destinationDataAddress(destinationAddress)
                .traceContext(Map.of())
                .properties(properties)
                .build();

        try {
            // transfer the data asynchronously
            AsyncStreamingDataSink.AsyncResponseContext asyncResponseContext = callback -> {
                StreamingOutput output = t -> callback.outputStreamConsumer().accept(t);
                var resp = Response.ok(output).type(callback.mediaType()).build();
                return response.resume(resp);
            };
            var sink = new AsyncStreamingDataSink(asyncResponseContext, executorService);

            transferService.transfer(flowRequest, sink).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    response.resume(unexpectedFailure(throwable));
                } else {
                    result.onSuccess(response::resume)
                            .onFailure(failure -> response.resume(failedResponse(failure)));
                }
            });
        } catch (Exception e) {
            response.resume(unexpectedFailure(e));
        }
    }

    private Response failedResponse(StreamFailure failure) {
        var httpStatus = switch (failure.getReason()) {
            case NOT_FOUND -> NOT_FOUND;
            case NOT_AUTHORIZED -> UNAUTHORIZED;
            case GENERAL_ERROR -> INTERNAL_SERVER_ERROR;
        };
        return status(httpStatus).type(APPLICATION_JSON).build();
    }

    private Map<String, String> dataPlaneProperties(AssetRequest request) {
        var props = new HashMap<String, String>();
        Optional.ofNullable(request.getQueryParams()).ifPresent((queryParams) -> props.put(QUERY_PARAMS, queryParams));
        Optional.ofNullable(request.getPathSegments()).ifPresent((path) -> props.put(PATH, path));
        return props;
    }


    private DataAddress dataPlaneAddress(DataAddress edr) {
        // TODO the header scheme should be dynamic based on the `authType`
        //  https://github.com/eclipse-edc/Connector/blob/main/docs/developer/data-plane-signaling/data-plane-signaling-token-handling.md
        var endpoint = edr.getStringProperty("endpoint");
        var token = edr.getStringProperty("authorization");
        return HttpDataAddress.Builder.newInstance()
                .baseUrl(endpoint)
                .proxyQueryParams("true")
                .proxyPath("true")
                .property(HEADER_AUTHORIZATION, token)
                .build();
    }

    private DataAddress resolveEdr(AssetRequest request) {
        if (request.getTransferProcessId() != null) {
            var edr = edrService.resolveByTransferProcess(request.getTransferProcessId(), AUTO_REFRESH);
            if (edr.failed()) {
                throw new BadRequestException(EDR_ERROR_MESSAGE.formatted(request.getTransferProcessId(), edr.getFailureDetail()));
            }
            return edr.getContent();
        } else {
            var resolvedEdrs = edrService.query(toQuery(request));

            if (resolvedEdrs.failed()) {
                throw new BadRequestException(EDR_ERROR_MESSAGE.formatted(request.getTransferProcessId(), resolvedEdrs.getFailureDetail()));
            }

            var edrs = resolvedEdrs.getContent();
            if (edrs.isEmpty()) {
                throw new BadRequestException("No EDR for asset: " + request.getAssetId());
            } else if (edrs.size() > 1) {
                throw new PreconditionFailedException("More than one EDR for asset: " + request.getAssetId());
            }

            var edrEntry = edrs.get(0);

            var edr = edrService.resolveByTransferProcess(edrEntry.getTransferProcessId(), AUTO_REFRESH);
            if (edr.failed()) {
                throw new BadRequestException(EDR_ERROR_MESSAGE.formatted(edrEntry.getTransferProcessId(), edr.getFailureDetail()));
            }
            return edr.getContent();

        }
    }

    private QuerySpec toQuery(AssetRequest request) {
        var specBuilder = QuerySpec.Builder.newInstance();
        mappers.entrySet()
                .stream()
                .filter(entry -> entry.getValue().apply(request) != null)
                .forEach(entry -> specBuilder.filter(Criterion.criterion(entry.getKey(), "=", entry.getValue().apply(request))));

        var spec = specBuilder.build();

        if (spec.getFilterExpression().isEmpty()) {
            throw new BadRequestException("No Filter provided in the request");
        }
        return spec;
    }

    private Response unexpectedFailure(Throwable throwable) {
        monitor.severe("Error processing gateway request", throwable);
        return status(BAD_GATEWAY).entity(format("'%s'", throwable.getMessage())).type(APPLICATION_JSON).build();
    }

}
