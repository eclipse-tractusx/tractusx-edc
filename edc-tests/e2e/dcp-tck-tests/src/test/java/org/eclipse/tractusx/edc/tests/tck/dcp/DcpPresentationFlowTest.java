/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
 * Copyright (c) 2025 Metaform Systems Inc.
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

package org.eclipse.tractusx.edc.tests.tck.dcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspacetck.core.system.ConsoleMonitor;
import org.eclipse.dataspacetck.runtime.TckRuntime;
import org.eclipse.edc.connector.controlplane.profile.DataspaceProfileContextRegistryImpl;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ParticipantIdExtractionFunction;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.eclipse.tractusx.edc.tests.MockBdrsClient;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry.WILDCARD;
import static org.eclipse.edc.spi.result.Result.success;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@EndToEndTest
public class DcpPresentationFlowTest {
    private static final int CALLBACK_PORT = getFreePort();
    private static final int PROTOCOL_API_PORT = getFreePort();
    private static final int DID_SERVER_PORT = getFreePort();
    private static final String PROTOCOL_API_PATH = "/protocol";
    private static final String VERIFIER_DID = formatDid(DID_SERVER_PORT, "verifier");
    private static final SecureTokenService STS_MOCK = mock();
    private static final DataspaceProfileContextRegistry DATASPACE_PROFILE_CONTEXT_REGISTRY_SPY = spy(DataspaceProfileContextRegistryImpl.class);
    
    @RegisterExtension
    static final RuntimePerClassExtension RUNTIME = new RuntimePerClassExtension(
            new EmbeddedRuntime("Connector-under-test", ":edc-controlplane:edc-controlplane-base", ":edc-extensions:single-participant-vault")
                    .registerServiceMock(SecureTokenService.class, STS_MOCK)
                    .registerServiceMock(DataspaceProfileContextRegistry.class, DATASPACE_PROFILE_CONTEXT_REGISTRY_SPY)
                    .registerServiceMock(BdrsClient.class, new MockBdrsClient((s) -> s, (s) -> s))
                    .configurationProvider(DcpPresentationFlowTest::runtimeConfiguration));
    @RegisterExtension
    protected static WireMockExtension didServer = WireMockExtension.newInstance()
            .options(wireMockConfig().port(DID_SERVER_PORT))
            .build();
    private ECKey verifierKey;

    @BeforeEach
    void setUp(TrustedIssuerRegistry trustedIssuerRegistry) throws JOSEException {
        verifierKey = generateEcKey();
        trustedIssuerRegistry.register(new Issuer(formatDid(CALLBACK_PORT, "issuer"), Map.of()),  WILDCARD);
        configureDidMock();
        configureStsMock();
        configureIdExtractionMock();
    }

    @DisplayName("Run TCK Presentation Flow tests")
    @Test
    void runPresentationFlowTests() {
        var monitor = new ConsoleMonitor(true, true);
        //Should be used DSP 2024-1 until it will not be updated in DCP TCK to 2025-1
        var triggerPath = PROTOCOL_API_PATH + "/2024/1/catalog/request";
        var holderDid = formatDid(CALLBACK_PORT, "holder");
        var thirdPartyDid = formatDid(CALLBACK_PORT, "thirdparty");
        var baseCallbackUrl = "http://localhost:%s".formatted(CALLBACK_PORT);
        var baseCallbackUri = URI.create(baseCallbackUrl);
        var result = TckRuntime.Builder.newInstance()
                .properties(Map.of(
                        "dataspacetck.callback.address", baseCallbackUrl,
                        "dataspacetck.host", baseCallbackUri.getHost(),
                        "dataspacetck.port", String.valueOf(baseCallbackUri.getPort()),
                        "dataspacetck.launcher", "org.eclipse.dataspacetck.dcp.system.DcpSystemLauncher",
                        "dataspacetck.did.verifier", VERIFIER_DID,
                        "dataspacetck.did.holder", holderDid,
                        "dataspacetck.did.thirdparty", thirdPartyDid,
                        "dataspacetck.vpp.trigger.endpoint", "http://localhost:%s%s".formatted(PROTOCOL_API_PORT, triggerPath)
                ))
                .monitor(monitor)
                .addPackage("org.eclipse.dataspacetck.dcp.verification.presentation.verifier")
                .build()
                .execute();

        monitor.enableBold().message("DCP Tests done: %s succeeded, %s failed".formatted(
                result.getTestsSucceededCount(), result.getTotalFailureCount()
        )).resetMode();

        assertThat(result.getFailures()).withFailMessage(errorMessageSupplier(result)).isEmpty();
    }

    private @NotNull Supplier<String> errorMessageSupplier(TestExecutionSummary result) {
        return () -> result.getFailures().stream()
                .map(f -> "- " + f.getTestIdentifier().getDisplayName() + " (" + f.getException() + ")")
                .collect(Collectors.joining("\n"));
    }

    private ECKey generateEcKey() throws JOSEException {
        return new ECKeyGenerator(Curve.P_256)
                .keyID(VERIFIER_DID + "#verifier-key1")
                .generate();
    }

    private void configureDidMock() {
        didServer.stubFor(get(urlPathEqualTo("/verifier/did.json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createDidDocumentJson()
        )));
    }

    private void configureStsMock() {
        when(STS_MOCK.createToken(any(), anyMap(), isNull()))
                .thenAnswer(i -> {
                    Map<String, Object> claims = new HashMap<>(i.getArgument(1));
                    var header = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(verifierKey.getKeyID()).build();
                    var claimsSet = new JWTClaimsSet.Builder(JWTClaimsSet.parse(claims))
                            .jwtID(UUID.randomUUID().toString())
                            .build();

                    var jwt = new SignedJWT(header, claimsSet);
                    jwt.sign(new ECDSASigner(verifierKey));
                    return success(TokenRepresentation.Builder.newInstance().token(jwt.serialize()).build());
                });
    }

    private void configureIdExtractionMock() {
        ParticipantIdExtractionFunction function = ct -> formatDid(CALLBACK_PORT, "holder");
        doReturn(function).when(DATASPACE_PROFILE_CONTEXT_REGISTRY_SPY).getIdExtractionFunction(any());
    }

    private String createDidDocumentJson() {
        var doc = DidDocument.Builder.newInstance()
                .id(VERIFIER_DID)
                .verificationMethod(
                    List.of(
                        VerificationMethod.Builder.newInstance()
                            .type("assertionMethod")
                            .controller(VERIFIER_DID)
                            .publicKeyJwk(verifierKey.toJSONObject())
                            .id(verifierKey.getKeyID())
                            .build()
                    )
                )
                .service(
                    List.of(
                        new Service(
                            UUID.randomUUID().toString(),
                            "CredentialService",
                            "https://example.com/credentialservice"
                        )
                    )
                )
                .build();
        try {
            return new ObjectMapper().writeValueAsString(doc);
        } catch (JsonProcessingException e) {
            throw new AssertionError(e);
        }
    }

    private static String formatDid(int port, String role) {
        return String.format("did:web:localhost%%3A%d:%s", port, role);
    }

    private static Config runtimeConfiguration() {
        return ConfigFactory.fromMap(new HashMap<>() {
            {
                put("edc.iam.accesstoken.jti.validation", "true");
                put("edc.iam.did.web.use.https", "false");
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.protocol.path", PROTOCOL_API_PATH);
                put("web.http.protocol.port", String.valueOf(PROTOCOL_API_PORT));
                put("edc.participant.id", "id");
                put("edc.iam.issuer.id", VERIFIER_DID);
                put("edc.iam.sts.oauth.token.url", "https://example.com/token");
                put("edc.iam.sts.oauth.client.id", "test-client-id");
                put("edc.iam.sts.oauth.client.secret.alias", "test-secret-alias");
                put("tx.edc.iam.iatp.bdrs.server.url", "http://sts.example.com");
                //register a default scope https://github.com/eclipse-dataspacetck/dcp-tck?tab=readme-ov-file#232-required-configuration
                put("tx.edc.iam.iatp.default-scopes.holderIdentifier.alias", "org.eclipse.dspace.dcp.vc.type");
                put("tx.edc.iam.iatp.default-scopes.holderIdentifier.type", "MembershipCredential");
                put("tx.edc.iam.iatp.default-scopes.holderIdentifier.operation", "read");
                put("tractusx.edc.participant.bpn", "bpn");
            }
        });
    }
}
