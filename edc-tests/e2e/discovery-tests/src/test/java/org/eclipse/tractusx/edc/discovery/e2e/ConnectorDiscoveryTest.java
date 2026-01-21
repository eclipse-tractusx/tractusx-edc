/*
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

package org.eclipse.tractusx.edc.discovery.e2e;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.eclipse.tractusx.edc.tests.MockBdrsClient;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.Runtimes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DSP_2025;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DSP_2025_PATH;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;

//@EndToEndTest
public class ConnectorDiscoveryTest {
    private static final String UNKNOWN_BPNL = "BPNL1234567890AB";
    private static final int DID_SERVER_PORT = getFreePort();
    private static final String LOCAL_PROVIDER_DID = "did:web:localhost%%3A%d:%s".formatted(DID_SERVER_PORT, "provider");

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

    private static final DidDocument RETURNED_DOCUMENT = DidDocument.Builder.newInstance()
            .id(PROVIDER_DID)
            .service(List.of(
                    new Service(PROVIDER_DID + "connector", "DataService", PROVIDER_FULL_DSP.getProtocolUrl()),
                    new Service(PROVIDER_DID + "cs", "CredentialService", "http://dontcare")))
            .build();

    @RegisterExtension
    static final RuntimeExtension CONSUMER_RUNTIME = Runtimes.discoveryRuntimeFullDsp(CONSUMER)
            .registerServiceMock(BdrsClient.class, new MockBdrsClient(ConnectorDiscoveryTest::resolveProviderDid,
                    ConnectorDiscoveryTest::resolveProviderBpn));

    @RegisterExtension
    protected static WireMockExtension didServer = WireMockExtension.newInstance()
            .options(wireMockConfig().port(DID_SERVER_PORT))
            .build();

//
//    @RegisterExtension
//    static final RuntimeExtension CONSUMER_RUNTIME_DID = Runtimes.discoveryRuntimeFullDsp(CONSUMER)
//            .registerServiceMock(DidResolverRegistry.class, new MockDidResolverRegistry(RETURNED_DOCUMENT));

    @RegisterExtension
    static final RuntimeExtension PROVIDER_RUNTIME_FULL_DSP = Runtimes.discoveryRuntimeFullDsp(PROVIDER_FULL_DSP);

    @RegisterExtension
    static final RuntimeExtension PROVIDER_RUNTIME_DSP_V08 = Runtimes.discoveryRuntimeDsp08(PROVIDER_DSP_V08);

    private static String resolveProviderDid(String bpn) {
        if (Objects.equals(bpn, UNKNOWN_BPNL)) {
            return null;
        }
        return PROVIDER_FULL_DSP.getDid();
    }

    private static String resolveProviderBpn(String did) {
        return PROVIDER_FULL_DSP.getBpn();
    }

    private void configureDidMock() throws JsonProcessingException {
        didServer.stubFor(get(urlPathEqualTo("/provider/did.json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(new ObjectMapper().writerFor(DidDocument.class).writeValueAsString(RETURNED_DOCUMENT))));
    }

    @BeforeEach
    void setUp() throws Exception {
        configureDidMock();
    }

    //@Test
    void discoveryShouldReturn2025DspParams() {

        var requestBody = createRequestBody(PROVIDER_FULL_DSP.getDid(), emptyList());

        var response = CONSUMER.discoverConnectorServices(requestBody);

        var body = response.statusCode(400)
                .extract().body().asString();

        assertThat(body)
                .isNotNull()
                .contains("Just to test it");
    }

    //    @Test
    //    void discoveryShouldReturn08DspParams_whenDidNotResolvable() {
    //
    //        var requestBody = createRequestBody("unresolvableBpnl", PROVIDER_FULL_DSP.getProtocolUrl());
    //
    //        var response = CONSUMER.discoverDspParameters(requestBody);
    //
    //        var body = response.statusCode(200)
    //                .extract().body().asString();
    //
    //        assertThat(body)
    //                .isNotNull()
    //                .contains("\"counterPartyAddress\":\"" + PROVIDER_FULL_DSP.getProtocolUrl())
    //                .contains("\"counterPartyId\":\"" + "unresolvableBpnl" + "\"")
    //                .contains("\"protocol\":\"" + "dataspace-protocol-http" + "\"");
    //
    //    }
    //
    //    @Test
    //    void discoveryShouldReturn08DspParams_whenDsp2025NotAvailable() {
    //
    //        var requestBody = createRequestBody(PROVIDER_DSP_V08.getBpn(), PROVIDER_DSP_V08.getProtocolUrl());
    //
    //        var response = CONSUMER.discoverDspParameters(requestBody);
    //
    //        var body = response.statusCode(200)
    //                .extract().body().asString();
    //
    //        assertThat(body)
    //                .isNotNull()
    //                .contains("\"counterPartyAddress\":\"" + PROVIDER_DSP_V08.getProtocolUrl())
    //                .contains("\"counterPartyId\":\"" + PROVIDER_DSP_V08.getBpn() + "\"")
    //                .contains("\"protocol\":\"" + "dataspace-protocol-http" + "\"");
    //
    //    }
    //
    //    @Test
    //    void discoveryShouldReturn400_ifRequestHasMissingProps() {
    //
    //        var requestBody = createObjectBuilder()
    //                .add(CONTEXT, createObjectBuilder().add("edc", EDC_NAMESPACE).add("tx", TX_NAMESPACE))
    //                .add(TYPE, "tx:ConnectorDiscoveryRequest")
    //                .add("edc:counterPartyAddress", PROVIDER_FULL_DSP.getProtocolUrl())
    //                .build();
    //
    //        var response = CONSUMER.discoverDspParameters(requestBody);
    //
    //        var body = response.statusCode(400)
    //                .extract().body().asString();
    //
    //        assertThat(body)
    //                .isNotNull()
    //                .contains("mandatory value 'https://w3id.org/tractusx/v0.0.1/ns/bpnl' is missing or it is blank");
    //
    //    }
    //
    //    @Test
    //    void discoveryShouldReturn500_ifMetadaEndpointNotReachable() {
    //
    //        var requestBody = createRequestBody(PROVIDER_FULL_DSP.getBpn(), PROVIDER_FULL_DSP.getProtocolUrl() + "/not-existing");
    //
    //        var response = CONSUMER.discoverDspParameters(requestBody);
    //
    //        var body = response.statusCode(500)
    //                .extract().body().asString();
    //
    //        assertThat(body)
    //                .isNotNull()
    //                .contains("Counter party well-known endpoint has failed with status 404 and message: Not Found");
    //
    //    }
    //
    //    @Test
    //    void discoveryShouldReturn500_whenProviderEndpointNotReachable() {
    //
    //        var requestBody = createRequestBody(PROVIDER_FULL_DSP.getBpn(), "http://non-existing-provider.com");
    //
    //        var response = CONSUMER.discoverDspParameters(requestBody);
    //
    //        var body = response.statusCode(500)
    //                .extract().body().asString();
    //
    //        assertThat(body)
    //                .isNotNull()
    //                .contains("An exception with the following message occurred while executing dsp version request:");
    //
    //    }
    //
    //    @Test
    //    void discoveryShouldReturn500_whenNoProtocolsAvailable() {
    //
    //        var requestBody = createRequestBody(PROVIDER_NO_PROTOCOLS.getBpn(), PROVIDER_NO_PROTOCOLS.getProtocolUrl());
    //
    //        var response = CONSUMER.discoverDspParameters(requestBody);
    //
    //
    //        var body = response.statusCode(500)
    //                .extract().body().asString();
    //
    //        assertThat(body)
    //                .isNotNull()
    //                .contains("No valid protocol version found for the counter party.");
    //
    //    }
    
    private JsonObject createRequestBody(String counterPartyId, List<String> knownConnectors) {
        var builder = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add("tx", TX_NAMESPACE))
                .add(TYPE, "tx:ConnectorServiceDiscoveryRequest")
                .add("edc:counterPartyId", counterPartyId);
        if (knownConnectors != null) {
            var arrayBuilder = createArrayBuilder();
            for (String known : knownConnectors) {
                arrayBuilder.add(known);
            }
            builder.add("tx:knownConnectors", arrayBuilder);
        }
        return builder.build();
    }

}

