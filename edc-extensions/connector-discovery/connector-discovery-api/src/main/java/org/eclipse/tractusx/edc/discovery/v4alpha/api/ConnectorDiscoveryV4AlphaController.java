/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2026 Cofinity-X GmbH
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
 */

package org.eclipse.tractusx.edc.discovery.v4alpha.api;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import org.eclipse.tractusx.edc.discovery.v4alpha.exceptions.UnexpectedResultApiException;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryRequest;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryService;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v4alpha/connectordiscovery")
public class ConnectorDiscoveryV4AlphaController implements ConnectorDiscoveryV4AlphaApi {

    private final ConnectorDiscoveryService connectorDiscoveryService;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonObjectValidatorRegistry validator;


    public ConnectorDiscoveryV4AlphaController(ConnectorDiscoveryService connectorDiscoveryService,
                                               TypeTransformerRegistry transformerRegistry,
                                               JsonObjectValidatorRegistry validator) {
        this.connectorDiscoveryService = connectorDiscoveryService;
        this.transformerRegistry = transformerRegistry;
        this.validator = validator;
    }

    @Path("/dspversionparams")
    @POST
    @Override
    public JsonObject discoverDspVersionParamsV4Alpha(JsonObject inputJson) {
        validator.validate(ConnectorParamsDiscoveryRequest.TYPE, inputJson)
                .orElseThrow(ValidationFailureException::new);

        var discoveryRequest = transformerRegistry.transform(inputJson, ConnectorParamsDiscoveryRequest.class);

        return connectorDiscoveryService.discoverVersionParams(discoveryRequest.getContent())
                .orElseThrow(failure -> new UnexpectedResultApiException(failure.getFailureDetail()));
    }

    @Path("/connectors")
    @POST
    @Override
    public void discoverConnectorServicesV4Alpha(JsonObject inputJson, @Suspended AsyncResponse response) {
        validator.validate(ConnectorDiscoveryRequest.TYPE, inputJson)
                .orElseThrow((ValidationFailureException::new));

        var request = transformerRegistry.transform(inputJson, ConnectorDiscoveryRequest.class);

        connectorDiscoveryService.discoverConnectors(request.getContent())
                .whenComplete((result, throwable) -> {
                    if ((throwable == null) && result.succeeded()) {
                        response.resume(result.getContent());
                    } else if (result.failed()) {
                        response.resume(new UnexpectedResultApiException(result.getFailureDetail()));
                    } else {
                        response.resume(throwable);
                    }
                });
    }
}

