/*
 *
 *   Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
 *
 *   See the NOTICE file(s) distributed with this work for additional
 *   information regarding copyright ownership.
 *
 *   This program and the accompanying materials are made available under the
 *   terms of the Apache License, Version 2.0 which is available at
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *   License for the specific language governing permissions and limitations
 *   under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.tractusx.edc.policy.cx;

import org.eclipse.edc.identitytrust.model.CredentialSubject;
import org.eclipse.edc.identitytrust.model.Issuer;
import org.eclipse.edc.identitytrust.model.VerifiableCredential;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CredentialFunctions {

    public static VerifiableCredential.Builder createCredential(String type, String version) {
        return VerifiableCredential.Builder.newInstance()
                .types(List.of("VerifiableCredential", type))
                .id(UUID.randomUUID().toString())
                .issuer(new Issuer(UUID.randomUUID().toString(), Map.of("prop1", "val1")))
                .expirationDate(Instant.now().plus(365, ChronoUnit.DAYS))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id("subject-id")
                        .claim("https://w3id.org/catenax/credentials/v1.0.0/holderIdentifier", "did:web:holder")
                        .claim("https://w3id.org/catenax/credentials/v1.0.0/contractVersion", version)
                        .claim("https://w3id.org/catenax/credentials/v1.0.0/contractTemplate", "https://public.catena-x.org/contracts/pcf.v1.pdf")
                        .build());
    }

    public static VerifiableCredential.Builder createPcfCredential() {
        return createCredential("PcfCredential", "1.0.0");
    }

    public static VerifiableCredential.Builder createDismantlerCredential(String... brands) {
        return createDismantlerCredential(Arrays.asList(brands), "vehicleDismantle");
    }

    public static VerifiableCredential.Builder createDismantlerCredential(Collection<String> brands, String... activityType) {
        var at = activityType.length == 1 ? activityType[0] : List.of(activityType);
        return VerifiableCredential.Builder.newInstance()
                .types(List.of("VerifiableCredential", "DismantlerCredential"))
                .id(UUID.randomUUID().toString())
                .issuer(new Issuer(UUID.randomUUID().toString(), Map.of("prop1", "val1")))
                .expirationDate(Instant.now().plus(365, ChronoUnit.DAYS))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id("subject-id")
                        .claim("https://w3id.org/catenax/credentials/v1.0.0/holderIdentifier", "did:web:holder")
                        .claim("https://w3id.org/catenax/credentials/v1.0.0/allowedVehicleBrands", brands)
                        .claim("https://w3id.org/catenax/credentials/v1.0.0/activityType", at)
                        .build());
    }

}
