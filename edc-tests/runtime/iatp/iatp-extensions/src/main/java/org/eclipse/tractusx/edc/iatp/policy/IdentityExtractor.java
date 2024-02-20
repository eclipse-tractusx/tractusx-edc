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

package org.eclipse.tractusx.edc.iatp.policy;

import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.agent.ParticipantAgentServiceExtension;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.edc.spi.agent.ParticipantAgent.PARTICIPANT_IDENTITY;

// TODO this is just a test identity extractor, the real one can be inspired by this but with final Identity credential
public class IdentityExtractor implements ParticipantAgentServiceExtension {

    private static final String VC_CLAIM = "vc";
    private static final String IDENTITY_CREDENTIAL = "MembershipCredential";
    private static final String IDENTITY_PROPERTY = "holderIdentifier";

    @Override
    public @NotNull Map<String, String> attributesFor(ClaimToken claimToken) {
        var credentials = getCredentialList(claimToken)
                .orElseThrow(failure -> new EdcException("Failed to fetch credentials from the claim token: %s".formatted(failure.getFailureDetail())));

        return credentials.stream()
                .filter(this::isIdentityCredential)
                .findFirst()
                .flatMap(this::getIdentifier)
                .map(identity -> Map.of(PARTICIPANT_IDENTITY, identity))
                .orElseThrow(() -> new EdcException("Failed to fetch %s property from %s credential".formatted(IDENTITY_PROPERTY, IDENTITY_CREDENTIAL)));


    }

    private boolean isIdentityCredential(VerifiableCredential verifiableCredential) {
        return verifiableCredential.getType().stream().anyMatch(t -> t.endsWith(IDENTITY_CREDENTIAL));
    }

    private Optional<String> getIdentifier(VerifiableCredential verifiableCredential) {
        return verifiableCredential.getCredentialSubject().stream()
                .flatMap(credentialSubject -> credentialSubject.getClaims().entrySet().stream())
                .filter(entry -> entry.getKey().endsWith(IDENTITY_PROPERTY))
                .map(Map.Entry::getValue)
                .map(String.class::cast)
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    private Result<List<VerifiableCredential>> getCredentialList(ClaimToken claimToken) {
        var vcListClaim = claimToken.getClaims().get(VC_CLAIM);

        if (vcListClaim == null) {
            return Result.failure("ClaimToken did not contain a '%s' claim.".formatted(VC_CLAIM));
        }
        if (!(vcListClaim instanceof List)) {
            return Result.failure("ClaimToken contains a '%s' claim, but the type is incorrect. Expected %s, got %s.".formatted(VC_CLAIM, List.class.getName(), vcListClaim.getClass().getName()));
        }
        var vcList = (List<VerifiableCredential>) vcListClaim;
        if (vcList.isEmpty()) {
            return Result.failure("ClaimToken contains a '%s' claim but it did not contain any VerifiableCredentials.".formatted(VC_CLAIM));
        }
        return Result.success(vcList);
    }
}
