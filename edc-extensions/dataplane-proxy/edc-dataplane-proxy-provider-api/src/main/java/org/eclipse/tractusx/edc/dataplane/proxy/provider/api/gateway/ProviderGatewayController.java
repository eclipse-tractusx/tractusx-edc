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

package org.eclipse.tractusx.edc.dataplane.proxy.provider.api.gateway;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.StreamingOutput;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.spi.pipeline.TransferService;
import org.eclipse.edc.connector.dataplane.spi.resolver.DataAddressResolver;
import org.eclipse.edc.connector.dataplane.util.sink.AsyncStreamingDataSink;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.authorization.AuthorizationHandlerRegistry;
import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.configuration.GatewayConfiguration;
import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.configuration.GatewayConfigurationRegistry;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static jakarta.ws.rs.core.Response.status;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.dataplane.proxy.provider.api.response.ResponseHelper.createMessageResponse;

/**
 * Implements the HTTP data proxy API.
 */
@Path("/" + ProviderGatewayController.GATEWAY_PATH)
public class ProviderGatewayController implements ProviderGatewayApi {
    protected static final String GATEWAY_PATH = "gateway";
    private static final String BASE_URL = EDC_NAMESPACE + "baseUrl";
    private static final String ASYNC = "async";

    private static final int ALIAS_SEGMENT = 1;
    private static final String BEARER_PREFIX = "Bearer ";

    private final TransferService transferService;
    private final GatewayConfigurationRegistry configurationRegistry;
    private final AuthorizationHandlerRegistry authorizationRegistry;

    private final DataAddressResolver dataAddressResolver;

    private final Monitor monitor;

    private final ExecutorService executorService;

    public ProviderGatewayController(TransferService transferService,
                                     DataAddressResolver dataAddressResolver,
                                     GatewayConfigurationRegistry configurationRegistry,
                                     AuthorizationHandlerRegistry authorizationRegistry,
                                     ExecutorService executorService,
                                     Monitor monitor) {
        this.transferService = transferService;
        this.dataAddressResolver = dataAddressResolver;
        this.configurationRegistry = configurationRegistry;
        this.authorizationRegistry = authorizationRegistry;
        this.executorService = executorService;
        this.monitor = monitor;
    }

    @GET
    @Path("/{paths: .+}")
    @Override
    public void requestAsset(@Context ContainerRequestContext context, @Suspended AsyncResponse response) {
        var tokens = context.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (tokens == null || tokens.isEmpty()) {
            response.resume(createMessageResponse(UNAUTHORIZED, "No bearer token", context.getMediaType()));
            return;
        }
        var token = tokens.get(0);
        if (!token.startsWith(BEARER_PREFIX)) {
            response.resume(createMessageResponse(UNAUTHORIZED, "Invalid bearer token", context.getMediaType()));
            return;
        } else {
            token = token.substring(BEARER_PREFIX.length());
        }


        var uriInfo = context.getUriInfo();
        var segments = uriInfo.getPathSegments();
        if (segments.size() < 3 || !GATEWAY_PATH.equals(segments.get(0).getPath())) {
            response.resume(createMessageResponse(BAD_REQUEST, "Invalid path", context.getMediaType()));
            return;
        }

        var alias = segments.get(ALIAS_SEGMENT).getPath();
        var configuration = configurationRegistry.getConfiguration(alias);
        if (configuration == null) {
            response.resume(createMessageResponse(NOT_FOUND, "Unknown path", context.getMediaType()));
            return;
        }

        var httpDataAddressResult = extractSourceDataAddress(token, configuration);
        HttpDataAddress httpDataAddress;

        if (httpDataAddressResult.succeeded()) {
            httpDataAddress = httpDataAddressResult.getContent();
        } else {
            monitor.debug("Request to token validation endpoint failed with errors: " + join(", ", httpDataAddressResult.getFailureMessages()));
            response.resume(createMessageResponse(UNAUTHORIZED, "Failed to decode data address", context.getMediaType()));
            return;
        }

        if (!configuration.getProxiedPath().startsWith(httpDataAddress.getBaseUrl())) {
            response.resume(createMessageResponse(NOT_FOUND, "Data address path not matched", context.getMediaType()));
            return;
        }

        // calculate the sub-path, which all segments after the GATEWAY segment, including the alias segment
        var subPath = segments.stream().skip(1).map(PathSegment::getPath).collect(joining("/"));
        if (!authenticate(token, configuration.getAuthorizationType(), subPath, context, response)) {
            return;
        }

        // calculate the request path, which all segments after the alias segment
        var requestPath = segments.stream().skip(2).map(PathSegment::getPath).collect(joining("/"));
        var flowRequest = createRequest(requestPath, configuration, httpDataAddress);

        // transfer the data asynchronously
        var sink = new AsyncStreamingDataSink(consumer -> response.resume((StreamingOutput) consumer::accept), executorService, monitor);

        try {
            transferService.transfer(flowRequest, sink).whenComplete((result, throwable) -> handleCompletion(response, result, throwable));
        } catch (Exception e) {
            reportError(response, e);
        }
    }

    private DataFlowRequest createRequest(String subPath, GatewayConfiguration configuration, HttpDataAddress httpDataAddress) {
        var path = configuration.getProxiedPath() + "/" + subPath;

        var sourceAddressBuilder = HttpDataAddress.Builder.newInstance()
                .property(BASE_URL, path);

        httpDataAddress.getAdditionalHeaders().forEach(sourceAddressBuilder::addAdditionalHeader);

        var destinationAddress = DataAddress.Builder.newInstance()
                .type(ASYNC)
                .build();

        return DataFlowRequest.Builder.newInstance()
                .processId(randomUUID().toString())
                .trackable(false)
                .sourceDataAddress(sourceAddressBuilder.build())
                .destinationDataAddress(destinationAddress)
                .traceContext(Map.of())
                .build();
    }

    private boolean authenticate(String token, String authType, String subPath, ContainerRequestContext context, AsyncResponse response) {
        var handler = authorizationRegistry.getHandler(authType);
        if (handler == null) {
            var correlationId = randomUUID().toString();
            monitor.severe(format("Authentication handler not configured for type: %s [id: %s]", authType, correlationId));
            response.resume(createMessageResponse(INTERNAL_SERVER_ERROR, format("Internal server error: %s", correlationId), context.getMediaType()));
            return false;
        }

        var authResponse = handler.authorize(token, subPath);
        if (authResponse.failed()) {
            response.resume(status(UNAUTHORIZED).build());
            return false;
        }
        return true;
    }

    /**
     * Handles a request completion, checking for errors. If no errors are present, nothing needs to be done as the response will have already been written to the client.
     */
    private void handleCompletion(AsyncResponse response, StreamResult<Object> result, Throwable throwable) {
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
                default:
                    throw new IllegalStateException("Unexpected value: " + result.reason());
            }
        } else if (throwable != null) {
            reportError(response, throwable);
        }
    }

    /**
     * Reports an error to the client. On the provider side, the error is reported as a {@code INTERNAL_SERVER_ERROR} since the provider data plane is considered an origin server
     * even though it may delegate requests to other internal sources.
     */
    private void reportError(AsyncResponse response, Throwable throwable) {
        monitor.severe("Error processing gateway request", throwable);
        var entity = status(INTERNAL_SERVER_ERROR).entity(format("'%s'", throwable.getMessage())).type(APPLICATION_JSON).build();
        response.resume(entity);
    }


    private Result<HttpDataAddress> extractSourceDataAddress(String token, GatewayConfiguration configuration) {
        return dataAddressResolver.resolve(token).map(dataAddress -> mapToHttpDataAddress(dataAddress, configuration, token));
    }

    private HttpDataAddress mapToHttpDataAddress(DataAddress dataAddress, GatewayConfiguration configuration, String token) {
        var builder = HttpDataAddress.Builder.newInstance().copyFrom(dataAddress);
        if (configuration.isForwardEdrToken()) {
            builder.addAdditionalHeader(configuration.getForwardEdrTokenHeaderKey(), token);
        }
        return builder.build();
    }
}
