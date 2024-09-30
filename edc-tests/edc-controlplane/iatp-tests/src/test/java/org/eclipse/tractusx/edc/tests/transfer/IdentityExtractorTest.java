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
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.runtime.Runtimes.iatpRuntime;

/**
 * This test asserts that the ParticipantAgent's identity is determined by the "credentialSubject.holderIdentifier" property.
 * Due to how the extractors are used and registered, this must be tested using a fully-fledged runtime.
 */
@EndToEndTest
public class IdentityExtractorTest implements IatpParticipants {

    @RegisterExtension
    protected static final RuntimeExtension CONSUMER_RUNTIME = iatpRuntime(CONSUMER.getName(), CONSUMER.iatpConfiguration(PROVIDER), CONSUMER.getKeyPair());

    @Test
    void verifyCorrectParticipantAgentId(ParticipantAgentService participantAgentService) {
        var claimtoken = ClaimToken.Builder.newInstance()
                .claim("vc", List.of(createCredential().build()))
                .build();
        var agent = participantAgentService.createFor(claimtoken);

        assertThat(agent.getIdentity()).isEqualTo("the-holder");
    }

    @Test
    void verifyAgentId_whenNoMembershipCredential(ParticipantAgentService participantAgentService) {
        var claimtoken = ClaimToken.Builder.newInstance()
                .claim("vc", List.of(createCredential().types(List.of("VerifiableCredential")).build()))
                .build();
        assertThatThrownBy(() -> participantAgentService.createFor(claimtoken)).isInstanceOf(EdcException.class)
                .hasMessage("Required credential type 'MembershipCredential' not present in ClaimToken, cannot extract property 'holderIdentifier'");
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
