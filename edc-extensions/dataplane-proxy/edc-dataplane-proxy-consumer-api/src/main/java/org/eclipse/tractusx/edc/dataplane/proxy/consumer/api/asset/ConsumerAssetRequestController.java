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
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.spi.pipeline.TransferService;
import org.eclipse.edc.connector.dataplane.util.sink.AsyncStreamingDataSink;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset.model.AssetRequest;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

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
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

/**
 * Implements the HTTP proxy API.
 */
@Path("/aas")
@Produces(MediaType.APPLICATION_JSON)
public class ConsumerAssetRequestController implements ConsumerAssetRequestApi {
    public static final String BASE_URL = EDC_NAMESPACE + "baseUrl";
    private static final String HTTP_DATA = "HttpData";
    private static final String ASYNC_TYPE = "async";
    private static final String HEADER_AUTHORIZATION = "header:authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final EndpointDataReferenceCache edrCache;
    private final TransferService transferService;
    private final Monitor monitor;

    private final ExecutorService executorService;

    public ConsumerAssetRequestController(EndpointDataReferenceCache edrCache,
                                          TransferService transferService,
                                          ExecutorService executorService,
                                          Monitor monitor) {
        this.edrCache = edrCache;
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

        var sourceAddress = Optional.ofNullable(request.getEndpointUrl())
                .map(url -> gatewayAddress(url, edr))
                .orElseGet(() -> dataPlaneAddress(edr));


        var destinationAddress = DataAddress.Builder.newInstance()
                .type(ASYNC_TYPE)
                .build();


        var properties = Optional.ofNullable(request.getEndpointUrl())
                .map((url) -> Map.<String, String>of())
                .orElseGet(() -> dataPlaneProperties(request));

        var flowRequest = DataFlowRequest.Builder.newInstance()
                .processId(randomUUID().toString())
                .trackable(false)
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

            transferService.transfer(flowRequest, sink).whenComplete((result, throwable) -> handleCompletion(response, result, throwable));
        } catch (Exception e) {
            reportError(response, e);
        }
    }


    private Map<String, String> dataPlaneProperties(AssetRequest request) {
        var props = new HashMap<String, String>();
        Optional.ofNullable(request.getQueryParams()).ifPresent((queryParams) -> props.put(QUERY_PARAMS, queryParams));
        Optional.ofNullable(request.getPathSegments()).ifPresent((path) -> props.put(PATH, path));
        return props;
    }

    private DataAddress gatewayAddress(String url, EndpointDataReference edr) {
        return HttpDataAddress.Builder.newInstance()
                .baseUrl(url)
                .property(HEADER_AUTHORIZATION, BEARER_PREFIX + edr.getAuthCode())
                .build();
    }

    private DataAddress dataPlaneAddress(EndpointDataReference edr) {
        return HttpDataAddress.Builder.newInstance()
                .baseUrl(edr.getEndpoint())
                .proxyQueryParams("true")
                .proxyPath("true")
                .property(HEADER_AUTHORIZATION, edr.getAuthCode())
                .build();
    }

    private EndpointDataReference resolveEdr(AssetRequest request) {
        if (request.getTransferProcessId() != null) {
            var edr = edrCache.resolveReference(request.getTransferProcessId());
            if (edr == null) {
                throw new BadRequestException("No EDR for transfer process: " + request.getTransferProcessId());
            }
            return edr;
        } else {
            var resolvedEdrs = edrCache.referencesForAsset(request.getAssetId(), request.getProviderId());
            if (resolvedEdrs.isEmpty()) {
                throw new BadRequestException("No EDR for asset: " + request.getAssetId());
            } else if (resolvedEdrs.size() > 1) {
                throw new PreconditionFailedException("More than one EDR for asset: " + request.getAssetId());
            }
            return resolvedEdrs.get(0);
        }
    }

    /**
     * Handles a request completion, checking for errors. If no errors are present, nothing needs to be done as the response will have already been written to the client.
     */
    private void handleCompletion(AsyncResponse response, StreamResult<Object> result, Throwable throwable) {
        if (result != null && result.failed()) {
            switch (result.reason()) {
                case NOT_FOUND -> response.resume(status(NOT_FOUND).type(APPLICATION_JSON).build());
                case NOT_AUTHORIZED -> response.resume(status(UNAUTHORIZED).type(APPLICATION_JSON).build());
                case GENERAL_ERROR -> response.resume(status(INTERNAL_SERVER_ERROR).type(APPLICATION_JSON).build());
                default -> throw new IllegalStateException("Unexpected value: " + result.reason());
            }
        } else if (throwable != null) {
            reportError(response, throwable);
        }
        if (result.succeeded()) {
            response.resume(result.getContent());
        }
    }

    /**
     * Reports an error to the client. On the consumer side, the error is reported as a {@code BAD_GATEWAY} since the consumer data plane acts as proxy.
     */
    private void reportError(AsyncResponse response, Throwable throwable) {
        monitor.severe("Error processing gateway request", throwable);
        var entity = status(BAD_GATEWAY).entity(format("'%s'", throwable.getMessage())).type(APPLICATION_JSON).build();
        response.resume(entity);
    }

}
