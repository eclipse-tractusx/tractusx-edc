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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_CREDENTIAL_NS;

public final class MembershipCredentialUtils {
    private static final String VC_CLAIM = "vc";
    private static final String MEMBERSHIP_CREDENTIAL = "MembershipCredential";
    private static final String HOLDER_IDENTIFIER = "holderIdentifier";

    private MembershipCredentialUtils() {
    }

    public static Optional<String> extractBpn(Map<String, Object> claims) {
        return extractBpn(claims, null);
    }

    public static Optional<String> extractBpn(Map<String, Object> claims, String did) {
        if (claims == null) {
            return Optional.empty();
        }

        var vcClaim = claims.get(VC_CLAIM);
        if (!(vcClaim instanceof Collection<?> credentials)) {
            return Optional.empty();
        }

        for (var credential : credentials) {
            if (credential instanceof VerifiableCredential vc) {
                if (!isMembershipCredential(vc.getType())) {
                    continue;
                }
                var bpn = vc.getCredentialSubject().stream()
                        .filter(subject -> matchesDid(subject, did))
                        .flatMap(subject -> subject.getClaims().entrySet().stream())
                        .filter(entry -> entry.getKey().endsWith(HOLDER_IDENTIFIER))
                        .map(Map.Entry::getValue)
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .findFirst();
                if (bpn.isPresent()) {
                    return bpn;
                }
            } else if (credential instanceof Map<?, ?> credentialMap) {
                if (!isMembershipCredential(credentialMap.get("type")) && !isMembershipCredential(credentialMap.get("@type"))) {
                    continue;
                }
                var bpn = extractBpnFromCredentialSubject(credentialMap.get("credentialSubject"), did);
                if (bpn.isPresent()) {
                    return bpn;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> extractBpnFromCredentialSubject(Object credentialSubject, String did) {
        if (credentialSubject instanceof Map<?, ?> subject) {
            if (!matchesDid(subject, did)) {
                return Optional.empty();
            }
            return subject.entrySet().stream()
                    .filter(entry -> entry.getKey() instanceof String key && key.endsWith(HOLDER_IDENTIFIER))
                    .map(Map.Entry::getValue)
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .findFirst();
        }
        if (credentialSubject instanceof Collection<?> subjects) {
            for (var subjectObject : subjects) {
                var bpn = extractBpnFromCredentialSubject(subjectObject, did);
                if (bpn.isPresent()) {
                    return bpn;
                }
            }
        }
        return Optional.empty();
    }

    private static boolean matchesDid(Map<?, ?> claims, String did) {
        if (did == null) {
            return true;
        }
        var subjectDid = claims.get("id");
        if (subjectDid == null) {
            subjectDid = claims.get(CX_CREDENTIAL_NS + "id");
        }
        if (subjectDid == null) {
            subjectDid = claims.get("@id");
        }
        return did.equals(subjectDid);
    }

    private static boolean matchesDid(CredentialSubject subject, String did) {
        if (did == null) {
            return true;
        }
        if (subject.getId() != null) {
            return did.equals(subject.getId());
        }
        return matchesDid(subject.getClaims(), did);
    }

    private static boolean isMembershipCredential(Object type) {
        if (type instanceof String value) {
            return value.endsWith(MEMBERSHIP_CREDENTIAL);
        }
        if (type instanceof Collection<?> types) {
            return types.stream().anyMatch(MembershipCredentialUtils::isMembershipCredential);
        }
        return false;
    }
}
