/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.tests.transfer;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.mockwebserver.MockResponse;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.lifecycle.ParticipantRuntime;
import org.eclipse.tractusx.edc.lifecycle.tx.iatp.DataspaceIssuer;
import org.eclipse.tractusx.edc.lifecycle.tx.iatp.IatpParticipant;
import org.eclipse.tractusx.edc.lifecycle.tx.iatp.SecureTokenService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_CREDENTIAL_NS;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;
import static org.eclipse.tractusx.edc.helpers.IatpHelperFunctions.configureParticipant;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.frameworkPolicy;
import static org.eclipse.tractusx.edc.helpers.TransferProcessHelperFunctions.createProxyRequest;
import static org.eclipse.tractusx.edc.tests.TestCommon.ASYNC_TIMEOUT;

@EndToEndTest
public class IatpHttpConsumerPullWithProxyInMemoryTest extends AbstractHttpConsumerPullWithProxyTest {

    protected static final DataspaceIssuer DATASPACE_ISSUER_PARTICIPANT = new DataspaceIssuer();
    protected static final SecureTokenService STS_PARTICIPANT = new SecureTokenService();

    protected static final IatpParticipant PLATO_IATP = new IatpParticipant(PLATO, STS_PARTICIPANT.stsUri());

    @RegisterExtension
    protected static final ParticipantRuntime PLATO_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:iatp:runtime-memory-iatp-ih",
            PLATO.getName(),
            PLATO.getBpn(),
            PLATO_IATP.iatpConfiguration(SOKRATES)
    );

    protected static final IatpParticipant SOKRATES_IATP = new IatpParticipant(SOKRATES, STS_PARTICIPANT.stsUri());

    @RegisterExtension
    protected static final ParticipantRuntime SOKRATES_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:iatp:runtime-memory-iatp-ih",
            SOKRATES.getName(),
            SOKRATES.getBpn(),
            SOKRATES_IATP.iatpConfiguration(PLATO)
    );

    @RegisterExtension
    protected static final ParticipantRuntime STS_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:iatp:runtime-memory-sts",
            STS_PARTICIPANT.getName(),
            STS_PARTICIPANT.getBpn(),
            STS_PARTICIPANT.stsConfiguration(SOKRATES_IATP, PLATO_IATP)
    );

    @BeforeAll
    static void prepare() {

        // create the DIDs cache
        var dids = new HashMap<String, DidDocument>();
        dids.put(DATASPACE_ISSUER_PARTICIPANT.didUrl(), DATASPACE_ISSUER_PARTICIPANT.didDocument());
        dids.put(SOKRATES_IATP.didUrl(), SOKRATES_IATP.didDocument());
        dids.put(PLATO_IATP.didUrl(), PLATO_IATP.didDocument());

        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, SOKRATES_IATP, SOKRATES_RUNTIME, dids, STS_RUNTIME);
        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, PLATO_IATP, PLATO_RUNTIME, dids, STS_RUNTIME);

    }


    @BeforeEach
    void setup() throws IOException {
        super.setup();
    }

    @Override
    protected JsonObject createContractPolicy(String bpn) {
        return frameworkPolicy(Map.of(CX_CREDENTIAL_NS + "Membership", "active"));
    }

    @DisplayName("Contract policy is fulfilled")
    @ParameterizedTest(name = "{1}")
    @ArgumentsSource(ContractPolicyProvider.class)
    void transferData_whenContractPolicyFulfilled(JsonObject contractPolicy, String description) throws IOException, InterruptedException {
        var assetId = "api-asset-1";
        var url = server.url("/mock/api");
        server.start();

        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";

        Map<String, Object> dataAddress = Map.of(
                "baseUrl", url.toString(),
                "type", "HttpData",
                "contentType", "application/json",
                "authKey", authCodeHeaderName,
                "authCode", authCode
        );

        PLATO.createAsset(assetId, Map.of(), dataAddress);

        var accessPolicyId = PLATO.createPolicyDefinition(createAccessPolicy(SOKRATES.getBpn()));
        var contractPolicyId = PLATO.createPolicyDefinition(contractPolicy);
        PLATO.createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);
        var transferProcessId = SOKRATES.requestAsset(PLATO, assetId, Json.createObjectBuilder().build(), createProxyRequest());

        var contractAgreementId = new AtomicReference<String>();
        var edr = new AtomicReference<EndpointDataReference>();

        // wait until transfer process completes
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var tpState = SOKRATES.getTransferProcessState(transferProcessId);
                    assertThat(tpState).isNotNull().isEqualTo(TransferProcessStates.STARTED.toString());
                });

        // wait until EDC is available on the consumer side
        server.enqueue(new MockResponse().setBody("test response").setResponseCode(200));
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    edr.set(SOKRATES.edrs().getDataReferenceFromBackend(transferProcessId));
                    assertThat(edr).isNotNull();
                });

        // pull data out of provider's backend service:
        // Cons-DP -> Prov-DP -> Prov-backend
        assertThat(SOKRATES.data().pullData(edr.get(), Map.of())).isEqualTo("test response");
        var rq = server.takeRequest();
        assertThat(rq.getHeader(authCodeHeaderName)).isEqualTo(authCode);
        assertThat(rq.getHeader("Edc-Contract-Agreement-Id")).isEqualTo(edr.get().getContractId());
        assertThat(rq.getHeader("Edc-Bpn")).isEqualTo(SOKRATES.getBpn());
        assertThat(rq.getMethod()).isEqualToIgnoringCase("GET");
    }

    private static class ContractPolicyProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
            return Stream.of(
                    Arguments.of(frameworkPolicy(Map.of(CX_POLICY_NS + "Membership", "active")), "MembershipCredential"),
                    Arguments.of(frameworkPolicy(Map.of(CX_POLICY_NS + "FrameworkAgreement.pcf", "active")), "PCF Use Case (legacy notation)"),
                    Arguments.of(frameworkPolicy(Map.of(CX_POLICY_NS + "FrameworkAgreement", "pcf")), "PCF Use Case (new notation)"),
                    Arguments.of(frameworkPolicy(Map.of(CX_POLICY_NS + "FrameworkAgreement", "sustainability")), "Sustainability Use Case (new notation)")
            );
        }
    }
}
