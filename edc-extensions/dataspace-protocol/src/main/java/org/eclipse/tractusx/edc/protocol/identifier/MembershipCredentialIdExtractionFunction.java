/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.protocol.identifier;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.protocol.spi.ParticipantIdExtractionFunction;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.core.utils.credentials.CredentialTypePredicate;

import java.util.List;
import java.util.Optional;

import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_CREDENTIAL_NS;

/**
 * Base class for participant id extraction from a {@link ClaimToken} based on the MembershipCredential.
 */
public abstract class MembershipCredentialIdExtractionFunction implements ParticipantIdExtractionFunction {
    
    private static final String VC_CLAIM = "vc";
    private static final String IDENTITY_CREDENTIAL = "MembershipCredential";
    
    private final CredentialTypePredicate typePredicate = new CredentialTypePredicate(CX_CREDENTIAL_NS, IDENTITY_CREDENTIAL);
    private final Monitor monitor;

    public MembershipCredentialIdExtractionFunction(Monitor monitor) {
        this.monitor = monitor.withPrefix(getClass().getSimpleName());
    }
    
    @Override
    public String apply(ClaimToken claimToken) {
        var credentials = getCredentialList(claimToken)
                .orElseThrow(failure -> {
                    var msg = "Failed to fetch credentials from the claim token: %s".formatted(failure.getFailureDetail());
                    monitor.severe(msg);
                    return new EdcException(msg);
                });
        
        return credentials.stream()
                .filter(typePredicate)
                .findFirst()
                .flatMap(this::getIdentifier)
                .orElseThrow(() -> {
                    var msg = "Required credential type '%s' not present in ClaimToken, cannot extract property '%s'".formatted(IDENTITY_CREDENTIAL, identityProperty());
                    monitor.severe(msg);
                    return new EdcException(msg);
                });
    }
    
    @SuppressWarnings("unchecked")
    private Result<List<VerifiableCredential>> getCredentialList(ClaimToken claimToken) {
        var vcListClaim = claimToken.getClaims().get(VC_CLAIM);
        
        if (vcListClaim == null) {
            var msg = "ClaimToken did not contain a '%s' claim.".formatted(VC_CLAIM);
            monitor.severe(msg);
            return Result.failure(msg);
        }
        if (!(vcListClaim instanceof List)) {
            var msg = "ClaimToken contains a '%s' claim, but the type is incorrect. Expected %s, got %s.".formatted(VC_CLAIM, List.class.getName(), vcListClaim.getClass().getName());
            monitor.severe(msg);
            return Result.failure(msg);
        }
        var vcList = (List<VerifiableCredential>) vcListClaim;
        if (vcList.isEmpty()) {
            var msg = "ClaimToken contains a '%s' claim but it did not contain any VerifiableCredentials.".formatted(VC_CLAIM);
            monitor.severe(msg);
            return Result.failure(msg);
        }
        return Result.success(vcList);
    }
    
    abstract String identityProperty();
    
    protected abstract Optional<String> getIdentifier(VerifiableCredential vc);
    
}
