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

package org.eclipse.tractusx.edc.discovery.api.v3;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import org.eclipse.tractusx.edc.discovery.api.ConnectorDiscoveryController;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v3/connectordiscovery")
public class ConnectorDiscoveryV3Controller implements ConnectorDiscoveryV3Api {

    private final ConnectorDiscoveryController delegate;

    public ConnectorDiscoveryV3Controller(ConnectorDiscoveryController delegate) {
        this.delegate = delegate;
    }

    @Path("/dspversionparams")
    @POST
    @Override
    public void discoverDspVersionParamsV3(JsonObject inputJson, @Suspended AsyncResponse response) {
        delegate.discoverDspVersionParams(inputJson, response);
    }

    @Path("/connectors")
    @POST
    @Override
    public void discoverConnectorServicesV3(JsonObject inputJson, @Suspended AsyncResponse response) {
        delegate.discoverConnectorServices(inputJson, response);
    }
}

