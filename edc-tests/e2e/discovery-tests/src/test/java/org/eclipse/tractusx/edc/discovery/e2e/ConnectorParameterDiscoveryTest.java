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

package org.eclipse.tractusx.edc.discovery.e2e;

import jakarta.json.JsonObject;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.eclipse.tractusx.edc.tests.MockBdrsClient;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.Runtimes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Objects;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DSP_2025;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DSP_2025_PATH;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;

@EndToEndTest
public class ConnectorParameterDiscoveryTest {
    private static final String UNKNOWN_BPNL = "BPNL1234567890AB";

    private static final TransferParticipant CONSUMER = TransferParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_DID)
            .bpn(CONSUMER_BPN)
            .protocol(DSP_2025)
            .protocolVersionPath(DSP_2025_PATH)
            .build();


    private static final TransferParticipant PROVIDER_FULL_DSP = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_DID)
            .bpn(PROVIDER_BPN)
            .build();

    private static final TransferParticipant PROVIDER_DSP_V08 = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME + "_V08")
            .id(PROVIDER_DID)
            .bpn(PROVIDER_BPN)
            .build();

    private static final TransferParticipant PROVIDER_NO_PROTOCOLS = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME + "_NO_PROTOCOLS")
            .id(PROVIDER_DID)
            .bpn(PROVIDER_BPN)
            .build();

    @RegisterExtension
    static final RuntimeExtension CONSUMER_RUNTIME = Runtimes.discoveryRuntimeFullDsp(CONSUMER)
            .registerServiceMock(BdrsClient.class, new MockBdrsClient(
                    ConnectorParameterDiscoveryTest::resolveProviderDid,
                    ConnectorParameterDiscoveryTest::resolveProviderBpn));


    @RegisterExtension
    static final RuntimeExtension PROVIDER_RUNTIME_FULL_DSP = Runtimes.discoveryRuntimeFullDsp(PROVIDER_FULL_DSP);

    @RegisterExtension
    static final RuntimeExtension PROVIDER_RUNTIME_DSP_V08 = Runtimes.discoveryRuntimeDsp08(PROVIDER_DSP_V08);

    @RegisterExtension
    static final RuntimeExtension PROVIDER_RUNTIME_NO_PROTOCOLS = Runtimes.discoveryRuntimeNoProtocols(PROVIDER_NO_PROTOCOLS);

    private static String resolveProviderDid(String bpn) {
        if (Objects.equals(bpn, UNKNOWN_BPNL)) {
            return null;
        }
        return PROVIDER_FULL_DSP.getDid();
    }

    private static String resolveProviderBpn(String did) {
        return PROVIDER_FULL_DSP.getBpn();
    }

    @Test
    void discoveryShouldReturn2025DspParams_BpnAsIdentifier() {

        var requestBody = createRequestBody(PROVIDER_FULL_DSP.getBpn(), PROVIDER_FULL_DSP.getProtocolUrl());

        var response = CONSUMER.discoverDspParameters(requestBody);

        var body = response.statusCode(200)
                .extract().body().asString();

        assertThat(body)
                .isNotNull()
                .contains("\"counterPartyAddress\":\"" + PROVIDER_FULL_DSP.getProtocolUrl() + "/2025-1\"")
                .contains("\"counterPartyId\":\"" + PROVIDER_FULL_DSP.getDid() + "\"")
                .contains("\"protocol\":\"" + "dataspace-protocol-http:2025-1" + "\"");
    }

    @Test
    void discoveryShouldReturn2025DspParams_DidAsIdentifier() {

        var requestBody = createRequestBody(PROVIDER_FULL_DSP.getDid(), PROVIDER_FULL_DSP.getProtocolUrl());

        var response = CONSUMER.discoverDspParameters(requestBody);

        var body = response.statusCode(200)
                .extract().body().asString();

        assertThat(body)
                .isNotNull()
                .contains("\"counterPartyAddress\":\"" + PROVIDER_FULL_DSP.getProtocolUrl() + "/2025-1\"")
                .contains("\"counterPartyId\":\"" + PROVIDER_FULL_DSP.getDid() + "\"")
                .contains("\"protocol\":\"" + "dataspace-protocol-http:2025-1" + "\"");
    }

    @Test
    void discoveryShouldReturn08DspParams_whenDsp2025NotAvailable_BpnAsIdentifier() {

        var requestBody = createRequestBody(PROVIDER_DSP_V08.getBpn(), PROVIDER_DSP_V08.getProtocolUrl());

        var response = CONSUMER.discoverDspParameters(requestBody);

        var body = response.statusCode(200)
                .extract().body().asString();

        assertThat(body)
                .isNotNull()
                .contains("\"counterPartyAddress\":\"" + PROVIDER_DSP_V08.getProtocolUrl())
                .contains("\"counterPartyId\":\"" + PROVIDER_DSP_V08.getBpn() + "\"")
                .contains("\"protocol\":\"" + "dataspace-protocol-http" + "\"");
    }

    @Test
    void discoveryShouldReturn08DspParams_whenDsp2025NotAvailable_DidAsIdentifier() {

        var requestBody = createRequestBody(PROVIDER_DSP_V08.getDid(), PROVIDER_DSP_V08.getProtocolUrl());

        var response = CONSUMER.discoverDspParameters(requestBody);

        var body = response.statusCode(200)
                .extract().body().asString();

        assertThat(body)
                .isNotNull()
                .contains("\"counterPartyAddress\":\"" + PROVIDER_DSP_V08.getProtocolUrl())
                .contains("\"counterPartyId\":\"" + PROVIDER_DSP_V08.getBpn() + "\"")
                .contains("\"protocol\":\"" + "dataspace-protocol-http" + "\"");
    }

    @Test
    void discoveryShouldReturn400_whenDidNotResolvable() {

        var requestBody = createRequestBody(UNKNOWN_BPNL, PROVIDER_FULL_DSP.getProtocolUrl());

        var response = CONSUMER.discoverDspParameters(requestBody);

        var body = response.statusCode(400)
                .extract().body().asString();

        assertThat(body)
                .isNotNull()
                .contains("cannot be mapped to a did");
    }

    @Test
    void discoveryShouldReturn400_ifRequestHasMissingProps() {

        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add("edc", EDC_NAMESPACE).add("tx", TX_NAMESPACE))
                .add(TYPE, "tx:ConnectorDiscoveryRequest")
                .add("edc:counterPartyAddress", PROVIDER_FULL_DSP.getProtocolUrl())
                .build();

        var response = CONSUMER.discoverDspParameters(requestBody);

        var body = response.statusCode(400)
                .extract().body().asString();

        assertThat(body)
                .isNotNull()
                .contains("Neither 'counterPartyId' nor 'bpnl' property given");
    }

    @Test
    void discoveryShouldReturn502_ifMetadaEndpointNotReachable() {

        var requestBody = createRequestBody(PROVIDER_FULL_DSP.getBpn(), PROVIDER_FULL_DSP.getProtocolUrl() + "/not-existing");

        var response = CONSUMER.discoverDspParameters(requestBody);

        var body = response.statusCode(502)
                .extract().body().asString();

        assertThat(body)
                .isNotNull()
                .contains("Counterparty well-known endpoint has failed with status 404 and message: Not Found");
    }

    @Test
    void discoveryShouldReturn500_whenProviderEndpointNotReachable() {

        var requestBody = createRequestBody(PROVIDER_FULL_DSP.getBpn(), "http://non-existing-provider.com");

        var response = CONSUMER.discoverDspParameters(requestBody);

        var body = response.statusCode(500)
                .extract().body().asString();
    }

    @Test
    void discoveryShouldReturn400_whenNoProtocolsAvailable() {

        var requestBody = createRequestBody(PROVIDER_NO_PROTOCOLS.getBpn(), PROVIDER_NO_PROTOCOLS.getProtocolUrl());

        var response = CONSUMER.discoverDspParameters(requestBody);

        var body = response.statusCode(400)
                .extract().body().asString();

        assertThat(body)
                .isNotNull()
                .contains("The counterparty does not support any of the expected protocol versions");
    }

    private JsonObject createRequestBody(String counterPartyId, String counterPartyAddress) {
        return createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add("edc", EDC_NAMESPACE).add("tx", TX_NAMESPACE))
                .add(TYPE, "tx:ConnectorDiscoveryRequest")
                .add("edc:counterPartyId", counterPartyId)
                .add("edc:counterPartyAddress", counterPartyAddress)
                .build();
    }

}

