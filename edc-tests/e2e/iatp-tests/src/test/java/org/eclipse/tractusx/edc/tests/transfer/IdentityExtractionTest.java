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

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.tractusx.edc.tests.participant.IatpParticipant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DSP_08;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DSP_2025;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.runtime.Runtimes.iatpRuntime;

/**
 * This test asserts that the ParticipantAgent's identity is determined by the "credentialSubject.holderIdentifier" property.
 * Due to how the extractors are used and registered, this must be tested using a fully-fledged runtime.
 */
@EndToEndTest
public class IdentityExtractionTest {

    private static final LazySupplier<URI> STS_URI = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort()));
    private static final IatpParticipant CONSUMER = IatpParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id("did:example:" + CONSUMER_NAME)
            .stsUri(STS_URI)
            .stsClientId(CONSUMER_BPN)
            .trustedIssuer("did:example:issuer")
            .bpn(CONSUMER_BPN)
            .build();

    @RegisterExtension
    private static final RuntimeExtension CONSUMER_RUNTIME = iatpRuntime(CONSUMER.getName(), CONSUMER.getKeyPair(), CONSUMER::getConfig);

    @Test
    void verifyCorrectParticipantAgentId_forDsp08(DataspaceProfileContextRegistry registry) {
        var claimtoken = ClaimToken.Builder.newInstance()
                .claim("vc", List.of(createCredential().build()))
                .build();
        
        var bpnExtractionFunction = registry.getIdExtractionFunction(DSP_08);
        var id = bpnExtractionFunction.apply(claimtoken);
        
        assertThat(id).isEqualTo("the-holder");
    }
    
    @Test
    void verifyCorrectParticipantAgentId_forDsp2025(DataspaceProfileContextRegistry registry) {
        var claimtoken = ClaimToken.Builder.newInstance()
                .claim("vc", List.of(createCredential().build()))
                .build();

        var bpnExtractionFunction = registry.getIdExtractionFunction(DSP_2025);
        var id = bpnExtractionFunction.apply(claimtoken);

        assertThat(id).isEqualTo("test-id");
    }

    @Test
    void verifyAgentId_whenNoMembershipCredential_forDsp08(DataspaceProfileContextRegistry registry) {
        var claimtoken = ClaimToken.Builder.newInstance()
                .claim("vc", List.of(createCredential().types(List.of("VerifiableCredential")).build()))
                .build();
        
        var bpnExtractionFunction = registry.getIdExtractionFunction(DSP_08);
        
        assertThatThrownBy(() -> bpnExtractionFunction.apply(claimtoken)).isInstanceOf(EdcException.class)
                .hasMessage("Required credential type 'MembershipCredential' not present in ClaimToken, cannot extract property 'holderIdentifier'");
    }
    
    @Test
    void verifyAgentId_whenNoMembershipCredential_forDsp2025(DataspaceProfileContextRegistry registry) {
        var claimtoken = ClaimToken.Builder.newInstance()
                .claim("vc", List.of(createCredential().types(List.of("VerifiableCredential")).build()))
                .build();
        
        var didExtractionFunction = registry.getIdExtractionFunction(DSP_2025);
        
        assertThatThrownBy(() -> didExtractionFunction.apply(claimtoken)).isInstanceOf(EdcException.class)
                .hasMessage("Required credential type 'MembershipCredential' not present in ClaimToken, cannot extract property 'id'");
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private VerifiableCredential.Builder createCredential() {
        return VerifiableCredential.Builder.newInstance()
                .types(List.of("VerifiableCredential", "MembershipCredential"))
                .id("test-id")
                .issuanceDate(Instant.now())
                .issuer(new Issuer("test-issuer", Map.of()))
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id("test-id")
                        .claim("holderIdentifier", "the-holder")
                        .build());
    }

}
