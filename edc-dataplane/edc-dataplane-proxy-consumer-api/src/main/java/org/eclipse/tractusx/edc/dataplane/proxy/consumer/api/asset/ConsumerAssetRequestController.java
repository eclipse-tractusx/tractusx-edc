/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.StreamingOutput;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.util.sink.AsyncStreamingDataSink;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset.model.AssetRequest;
import org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceCache;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.BAD_GATEWAY;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static jakarta.ws.rs.core.Response.status;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;

/**
 * Implements the HTTP proxy API.
 */
@Path("/aas")
public class ConsumerAssetRequestController implements ConsumerAssetRequestApi {
    private static final String HTTP_DATA = "HttpData";
    private static final String ASYNC_TYPE = "async";
    private static final String BASE_URL = "baseUrl";
    private static final String HEADER_AUTHORIZATION = "header:authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final EndpointDataReferenceCache edrCache;
    private final DataPlaneManager dataPlaneManager;
    private final Monitor monitor;

    private final ExecutorService executorService;

    public ConsumerAssetRequestController(EndpointDataReferenceCache edrCache,
                                          DataPlaneManager dataPlaneManager,
                                          ExecutorService executorService,
                                          Monitor monitor) {
        this.edrCache = edrCache;
        this.dataPlaneManager = dataPlaneManager;
        this.executorService = executorService;
        this.monitor = monitor;
    }

    @POST
    @Path("/request")
    @Override
    public void requestAsset(AssetRequest request, @Suspended AsyncResponse response) {
        // resolve the EDR and add it to the request
        var edr = resolveEdr(request);

        var sourceAddress = DataAddress.Builder.newInstance()
                .type(HTTP_DATA)
                .property(BASE_URL, request.getEndpointUrl())
                .property(HEADER_AUTHORIZATION, BEARER_PREFIX + edr.getAuthCode())
                .build();

        var destinationAddress = DataAddress.Builder.newInstance()
                .type(ASYNC_TYPE)
                .build();

        var flowRequest = DataFlowRequest.Builder.newInstance()
                .processId(randomUUID().toString())
                .trackable(false)
                .sourceDataAddress(sourceAddress)
                .destinationDataAddress(destinationAddress)
                .traceContext(Map.of())
                .build();

        // transfer the data asynchronously
        var sink = new AsyncStreamingDataSink(consumer -> response.resume((StreamingOutput) consumer::accept), executorService, monitor);

        try {
            dataPlaneManager.transfer(sink, flowRequest).whenComplete((result, throwable) -> handleCompletion(response, result, throwable));
        } catch (Exception e) {
            reportError(response, e);
        }
    }

    private EndpointDataReference resolveEdr(AssetRequest request) {
        if (request.getTransferProcessId() != null) {
            var edr = edrCache.resolveReference(request.getTransferProcessId());
            if (edr == null) {
                throw new BadRequestException("No EDR for transfer process: " + request.getTransferProcessId());
            }
            return edr;
        } else {
            var resolvedEdrs = edrCache.referencesForAsset(request.getAssetId());
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
    private void handleCompletion(AsyncResponse response, StreamResult<Void> result, Throwable throwable) {
        if (result != null && result.failed()) {
            switch (result.reason()) {
                case NOT_FOUND:
                    response.resume(status(NOT_FOUND).type(APPLICATION_JSON).build());
                    break;
                case NOT_AUTHORIZED:
                    response.resume(status(UNAUTHORIZED).type(APPLICATION_JSON).build());
                    break;
                case GENERAL_ERROR:
                    response.resume(status(INTERNAL_SERVER_ERROR).type(APPLICATION_JSON).build());
                    break;
            }
        } else if (throwable != null) {
            reportError(response, throwable);
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
