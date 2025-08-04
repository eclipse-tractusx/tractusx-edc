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
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionService;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.tractusx.edc.discovery.models.ConnectorParamsDiscoveryRequest;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class ConnectorDiscoveryServiceImplTest {

    private final BdrsClient bdrsClient = mock();
    private final VersionService versionService = mock();
    private final ConnectorDiscoveryServiceImpl service = new ConnectorDiscoveryServiceImpl(bdrsClient, versionService);

    @Test
    void discoverRequestParams_shouldReturnDsp2025_whenDsp2025AvailableAndDidResolvable() {
        //given bpn available
        //given did resolvable
        //given dsp2025 available
        //should return dsp2025
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest("someBpnl", "someAddress");

        var expectedDid = "did:web:providerdid";

        var mockVersionResponseMock = Json.createObjectBuilder()
                .add("protocolVersions", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("version", "2025-1")
                                .add("path", "/somePath")
                                .add("binding", "someBinding"))).build();

        var expectedJsonArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("connectors", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("counterPartyId", expectedDid)
                                .add("protocol", "dataspace-protocol-http:2025-1")
                                .add("counterPartyAddress", "someAddress/somePath")
                                .build())
                        .build())
                ).build();

        when(bdrsClient.resolve(paramsDiscoveryRequest.bpnl()))
                .thenReturn(expectedDid);
        when(versionService.requestVersions(any()))
                .thenReturn(CompletableFuture.completedFuture(StatusResult.success(mockVersionResponseMock.toString().getBytes())));

        var response = service.discover(paramsDiscoveryRequest);

        assertThat(response).isSucceeded();
        assertThat(response.getContent()).isEqualTo(expectedJsonArray);

    }

    @Test
    void discoverRequestParams_shoudReturnDsp08_whenDidCantBeResolved() {
        //given bpnl available
        //given did not resolvable
        //given dsp08 available
        //should return dsp08
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest("someBpnl", "someAddress");

        var mockVersionResponseMock = Json.createObjectBuilder()
                .add("protocolVersions", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("version", "0.8")
                                .add("path", "/")
                                .add("binding", "someBinding"))).build();

        when(bdrsClient.resolve(paramsDiscoveryRequest.bpnl()))
                .thenReturn(null);
        when(versionService.requestVersions(any()))
                .thenReturn(CompletableFuture.completedFuture(StatusResult.success(mockVersionResponseMock.toString().getBytes())));

        var expectedJsonArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("connectors", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("counterPartyId", "someBpnl")
                                .add("protocol", "dataspace-protocol-http")
                                .add("counterPartyAddress", "someAddress/")
                                .build())
                        .build())
                ).build();

        var response = service.discover(paramsDiscoveryRequest);

        assertThat(response).isSucceeded();
        assertThat(response.getContent()).isEqualTo(expectedJsonArray);
    }

    @Test
    void discoverRequestParams_shouldReturnEmpty(){
        //given bpn available
        //given did not resolvable
        //given dsp08 not available
        //should return empty array

    }

    @Test
    void discoverRequestParams_shouldReturnDsp08_whenDsp2025NotAvailableAndDsp08AvailableAndBpnAvailable(){
        //given bpn available
        //given did resolvable
        //given dsp2025 not available
        //given dsp08 available
        //should return dsp08
    }

    @Test
    void discoverRequestParams_shouldReturnDsp08_whenMetadataEndpointIsNotAvailableAndBpnlAvailable(){
        //given bpn available
        //given did resolvable
        //given metadata endpoint not available
        //should return dsp08 with bpn
    }


}