/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2026 Cofinity-X GmbH
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

package org.eclipse.edc.iam.verifiablecredentials.rules;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.spi.result.Result;

import java.util.Collections;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * A class that implements the {@link CredentialValidationRule} interface and checks if a {@link VerifiableCredential} has a trusted issuer,
 * and if the credential type is supported for this issuer.
 */
public class HasValidIssuer implements CredentialValidationRule {
    private final TrustedIssuerRegistry trustedIssuerRegistry;

    public HasValidIssuer(TrustedIssuerRegistry trustedIssuerRegistry) {
        this.trustedIssuerRegistry = trustedIssuerRegistry;
    }

    @Override
    public Result<Void> apply(VerifiableCredential credential) {
        var issuer = credential.getIssuer();
        if (issuer.id() == null) {
            return failure("Issuer did not contain an 'id' field.");
        }

        var supportedTypes = trustedIssuerRegistry.getSupportedTypes(issuer);
        return !supportedTypes.contains(TrustedIssuerRegistry.WILDCARD) && Collections.disjoint(credential.getType(), supportedTypes) ?
                failure("Credential types '%s' are not supported for issuer '%s'".formatted(credential.getType(), issuer.id())) :
                success();
    }

}
