/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2025 SAP SE
 * Copyright (c) 2026 Materna SE
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

package org.constructx.edc.policy.constructx;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.constructx.edc.policy.constructx.ConstructxPolicyConstants.CONSTRUCTX_CREDENTIAL_NS;

public class CredentialFunctions {

    public static VerifiableCredential.Builder createCredential(String type) {
        return VerifiableCredential.Builder.newInstance()
                .types(List.of(CONSTRUCTX_CREDENTIAL_NS + "VerifiableCredential", CONSTRUCTX_CREDENTIAL_NS + type))
                .id(UUID.randomUUID().toString())
                .issuer(new Issuer(UUID.randomUUID().toString(), Map.of("prop1", "val1")))
                .expirationDate(Instant.now().plus(365, ChronoUnit.DAYS))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id("subject-id")
                        .claim("arbitrary", "claim")
                        .build());
    }

    public static VerifiableCredential.Builder createMembershipCredential() {
        return VerifiableCredential.Builder.newInstance()
                .types(List.of(
                        CONSTRUCTX_CREDENTIAL_NS + "VerifiableCredential",
                        CONSTRUCTX_CREDENTIAL_NS + "MembershipCredential",
                        CONSTRUCTX_CREDENTIAL_NS + "ConstructXMembershipCredential"
                ))
                .id(UUID.randomUUID().toString())
                .issuer(new Issuer(UUID.randomUUID().toString(), Map.of("prop1", "val1")))
                .expirationDate(Instant.now().plus(365, ChronoUnit.DAYS))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id("subject-id")
                        .claim("holderIdentifier", "did:web:holder")
                        .build());
    }

    private CredentialFunctions() {
    }
}
