/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.core.utils.credentials;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MembershipCredentialUtilsTest {
    private static final String DID = "did:web:consumer";
    private static final String BPN = "BPNL000000000001";

    @Test
    void shouldExtractBpnFromVerifiableCredential() {
        var credential = VerifiableCredential.Builder.newInstance()
                .types(List.of("VerifiableCredential", "MembershipCredential"))
                .issuer(new Issuer("did:web:issuer", Map.of()))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .claim("id", DID)
                        .claim("holderIdentifier", BPN)
                        .build())
                .build();

        assertThat(MembershipCredentialUtils.extractBpn(Map.of("vc", List.of(credential)), DID))
                .contains(BPN);
    }

    @Test
    void shouldExtractBpnFromSqlMapCredential() {
        var claims = Map.<String, Object>of("vc", List.of(Map.of(
                "type", List.of("VerifiableCredential", "MembershipCredential"),
                "credentialSubject", Map.of("id", DID, "holderIdentifier", BPN))));

        assertThat(MembershipCredentialUtils.extractBpn(claims, DID))
                .contains(BPN);
    }

    @Test
    void shouldNotExtractBpnFromCredentialForAnotherDid() {
        var claims = Map.<String, Object>of("vc", List.of(Map.of(
                "type", "MembershipCredential",
                "credentialSubject", Map.of("id", "did:web:other", "holderIdentifier", BPN))));

        assertThat(MembershipCredentialUtils.extractBpn(claims, DID)).isEmpty();
    }
}
