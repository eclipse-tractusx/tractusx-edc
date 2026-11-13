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

import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.spi.result.Result;

/**
 * This class represents a rule that checks if a given VerifiableCredential is revoked based on a BitStringStatusList/StatusList2021 credential.
 * A credential is regarded as "not revoked" if:
 * <ul>
 *     <li>{@code Credential.credentialStatus.statusPurpose == StatusListCredential.credentialSubject.statusPurpose}</li>
 *     <li> value of the bit-string obtained from {@code StatusListCredential.encodedList} at the index obtained from {@code Credential.credentialStatus.statusIndex} is 1</li>
 * </ul>
 * <p>
 * All other situations, such as missing properties, invalid values etc. will be interpreted as "revoked".
 * Credentials that don't have a {@code credentialStatus} property are regarded as "not revoked".
 */
public class IsNotRevoked implements CredentialValidationRule {

    private final RevocationServiceRegistry revocationServiceRegistry;

    public IsNotRevoked(RevocationServiceRegistry revocationServiceRegistry) {
        this.revocationServiceRegistry = revocationServiceRegistry;
    }

    @Override
    public Result<Void> apply(VerifiableCredential credential) {
        return revocationServiceRegistry.checkValidity(credential);
    }
}
