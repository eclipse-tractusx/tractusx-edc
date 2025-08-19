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

public class DiscoveryTest {

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
            .registerServiceMock(BdrsClient.class, new MockBdrsClient(DiscoveryTest::resolveProviderDid, (s) -> s));


    @RegisterExtension
    static final RuntimeExtension PROVIDER_RUNTIME_FULL_DSP = Runtimes.discoveryRuntimeFullDsp(PROVIDER_FULL_DSP);

    @RegisterExtension
    static final RuntimeExtension PROVIDER_RUNTIME_DSP_V08 = Runtimes.discoveryRuntimeDsp08(PROVIDER_DSP_V08);

    @RegisterExtension
    static final RuntimeExtension PROVIDER_RUNTIME_NO_PROTOCOLS = Runtimes.discoveryRuntimeNoProtocols(PROVIDER_NO_PROTOCOLS);

    private static String resolveProviderDid(String bpn) {
        if (Objects.equals(bpn, "unresolvableBpnl")) {
            return null;
        }
        return PROVIDER_FULL_DSP.getDid();
    }

    @Test
    void discoveryShouldReturn2025DspParams() {

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
    void discoveryShouldReturn08DspParams_whenDidNotResolvable() {

        var requestBody = createRequestBody("unresolvableBpnl", PROVIDER_FULL_DSP.getProtocolUrl());

        var response = CONSUMER.discoverDspParameters(requestBody);

        var body = response.statusCode(200)
                .extract().body().asString();

        assertThat(body)
                .isNotNull()
                .contains("\"counterPartyAddress\":\"" + PROVIDER_FULL_DSP.getProtocolUrl() + "/\"")
                .contains("\"counterPartyId\":\"" + "unresolvableBpnl" + "\"")
                .contains("\"protocol\":\"" + "dataspace-protocol-http" + "\"");

    }

    @Test
    void discoveryShouldReturn08DspParams_whenDsp2025NotAvailable() {

        var requestBody = createRequestBody(PROVIDER_DSP_V08.getBpn(), PROVIDER_DSP_V08.getProtocolUrl());

        var response = CONSUMER.discoverDspParameters(requestBody);

        var body = response.statusCode(200)
                .extract().body().asString();

        assertThat(body)
                .isNotNull()
                .contains("\"counterPartyAddress\":\"" + PROVIDER_DSP_V08.getProtocolUrl() + "/\"")
                .contains("\"counterPartyId\":\"" + PROVIDER_DSP_V08.getBpn() + "\"")
                .contains("\"protocol\":\"" + "dataspace-protocol-http" + "\"");

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
                .contains("mandatory value 'https://w3id.org/tractusx/v0.0.1/ns/bpnl' is missing or it is blank");

    }

    @Test
    void discoveryShouldReturn502_ifMetadaEndpointNotReachable() {

        var requestBody = createRequestBody(PROVIDER_FULL_DSP.getBpn(), PROVIDER_FULL_DSP.getProtocolUrl() + "/not-existing");

        var response = CONSUMER.discoverDspParameters(requestBody);

        var body = response.statusCode(502)
                .extract().body().asString();

        assertThat(body)
                .isNotNull()
                .contains("Counter party well-known endpoint has failed");

    }

    @Test
    void discoveryShouldReturn502_whenProviderEndpointNotReachable() {

        var requestBody = createRequestBody(PROVIDER_FULL_DSP.getBpn(), "http://non-existing.provider");

        var response = CONSUMER.discoverDspParameters(requestBody);

        var body = response.statusCode(502)
                .extract().body().asString();

        assertThat(body)
                .isNotNull()
                .contains("Timeout while waiting for the counter party to respond.");

    }

    @Test
    void discoveryShouldReturn500_whenNoProtocolsAvailable() {

        var requestBody = createRequestBody(PROVIDER_NO_PROTOCOLS.getBpn(), PROVIDER_NO_PROTOCOLS.getProtocolUrl());

        var response = CONSUMER.discoverDspParameters(requestBody);


        var body = response.statusCode(500)
                .extract().body().asString();

        assertThat(body)
                .isNotNull()
                .contains("No valid protocol version found for the counter party.");

    }

    private JsonObject createRequestBody(String bpn, String counterPartyAddress) {
        return createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add("edc", EDC_NAMESPACE).add("tx", TX_NAMESPACE))
                .add(TYPE, "tx:ConnectorDiscoveryRequest")
                .add("tx:bpnl", bpn)
                .add("edc:counterPartyAddress", counterPartyAddress)
                .build();
    }

}

