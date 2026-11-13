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
import org.eclipse.edc.spi.result.Result;

import java.time.Clock;

/**
 * Rule that verifies, that a credential is already valid ({@code issuanceDate} is before NOW), and that it is not yet expired.
 * {@code expirationDate} is not mandatory, so expiration is only checked if it is present.
 */
public class IsInValidityPeriod implements CredentialValidationRule {
    private final Clock clock;

    public IsInValidityPeriod(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Result<Void> apply(VerifiableCredential credential) {
        var now = clock.instant();
        // issuance date can not be null, due to builder validation
        if (credential.getIssuanceDate().isAfter(now)) {
            return Result.failure("Credential is not yet valid.");
        }
        if (credential.getExpirationDate() != null && credential.getExpirationDate().isBefore(now)) {
            return Result.failure("Credential expired.");
        }
        return Result.success();
    }
}
