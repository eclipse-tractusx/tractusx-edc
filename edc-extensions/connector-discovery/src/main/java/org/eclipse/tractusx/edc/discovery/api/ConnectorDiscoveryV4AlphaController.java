/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.discovery.api;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.tractusx.edc.discovery.service.ConnectorDiscoveryServiceImpl;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v4alpha/connectordiscovery")
public class ConnectorDiscoveryV4AlphaController implements ConnectorDiscoveryV4AlphaApi {

    private final ConnectorDiscoveryServiceImpl connectorDiscoveryService;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonObjectValidatorRegistry validator;
    private final Monitor monitor;


    public ConnectorDiscoveryV4AlphaController(ConnectorDiscoveryServiceImpl connectorDiscoveryService,
                                               TypeTransformerRegistry transformerRegistry,
                                               JsonObjectValidatorRegistry validator,
                                               Monitor monitor) {
        this.connectorDiscoveryService = connectorDiscoveryService;
        this.transformerRegistry = transformerRegistry;
        this.validator = validator;
        this.monitor = monitor;
    }

    @POST
    public JsonArray discoverConnectorV3(JsonObject querySpecJson) {
        monitor.severe("Connector Discovery V4 Alpha API is not implemented yet. Please use the V3 API instead.");
        return Json.createArrayBuilder().add(
                Json.createObjectBuilder()
                        .add("connectors", Json.createArrayBuilder().add(
                                Json.createObjectBuilder()
                                        .add("counterPartyId", "did:web:provider")
                                        .add("protocol", "dataspace-protocol-http:2025-1")
                        ))
        ).build();
    }

}
