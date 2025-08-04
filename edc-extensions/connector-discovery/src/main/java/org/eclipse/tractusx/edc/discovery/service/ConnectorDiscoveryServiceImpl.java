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

package org.eclipse.tractusx.edc.discovery.service;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.connector.controlplane.protocolversion.spi.ProtocolVersionRequest;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionService;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.tractusx.edc.discovery.models.ConnectorParamsDiscoveryRequest;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

import java.util.concurrent.ExecutionException;

public class ConnectorDiscoveryServiceImpl {

    private final BdrsClient bdrsClient;
    private final VersionService versionService;

    public ConnectorDiscoveryServiceImpl(BdrsClient bdrsClient, VersionService versionService) {
        this.bdrsClient = bdrsClient;
        this.versionService = versionService;

    }

    public ServiceResult<JsonArray> discover(ConnectorParamsDiscoveryRequest request) {

        var versionParameters = Json.createObjectBuilder();

        var protocolVersionRequest = ProtocolVersionRequest.Builder.newInstance()
                .protocol("dataspace-protocol-http")
                .counterPartyId(request.bpnl())
                .counterPartyAddress(request.counterPartyAddress())
                .build();

        try {
            var result = versionService.requestVersions(protocolVersionRequest).get();

            if (result.succeeded()) {
                var content = result.getContent();
                try (var reader = Json.createReader(new java.io.ByteArrayInputStream(content))) {
                    var jsonObject = reader.readObject();
                    var version = "0.8";
                    var firstResult = jsonObject.get("protocolVersions").asJsonArray().get(0).asJsonObject();
                    if (firstResult.containsKey("version")) {
                        version = firstResult.getString("version");
                    }
                    if ("2025-1".equals(version)) {
                        var did = bdrsClient.resolve(request.bpnl());
                        if (did != null) {
                            createDsp2025ResponseParameters(did, version, firstResult, request.counterPartyAddress(), versionParameters);
                        } else {
                            createDsp08ResponseParameters(request.bpnl(), version, firstResult, request.counterPartyAddress(), versionParameters);
                        }
                    } else if ("0.8".equals(version)) {
                        createDsp08ResponseParameters(request.bpnl(), version, firstResult, request.counterPartyAddress(), versionParameters);
                    }
                }

            }

        } catch (ExecutionException | InterruptedException e) {
            // TODO: handle specific exceptions
        }

        var message = Json.createArrayBuilder().add(
                Json.createObjectBuilder()
                        .add("connectors", Json.createArrayBuilder().add(versionParameters))
        ).build();

        return ServiceResult.success(message);
    }

    private void createDsp2025ResponseParameters(String did, String version, JsonObject firstResult, String counterPartyAddress, JsonObjectBuilder versionParameters) {

        if (firstResult.containsKey("path")) {
            versionParameters.add("counterPartyAddress", counterPartyAddress + firstResult.getString("path"));
        }

        if (version.equals("2025-1")) {
            versionParameters.add("protocol", "dataspace-protocol-http:2025-1");
        }

        versionParameters.add("counterPartyId", did);

    }

    private void createDsp08ResponseParameters(String bpnl, String version, JsonObject firstResult, String counterPartyAddress, JsonObjectBuilder versionParameters) {

        if (firstResult.containsKey("path")) {
            versionParameters.add("counterPartyAddress", counterPartyAddress + firstResult.getString("path"));
        }

        if (version.equals("0.8")) {
            versionParameters.add("protocol", "dataspace-protocol-http");
        }

        versionParameters.add("counterPartyId", bpnl);

    }


}
