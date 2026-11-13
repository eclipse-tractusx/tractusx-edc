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

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.spi.result.Result;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * This class implements the CredentialValidationRule interface and checks if all subject IDs in a
 * VerifiableCredential match an expected subject ID, which in practice is the DID of the holder of a VP.
 */
public class HasValidSubjectIds implements CredentialValidationRule {

    private final String expectedSubjectId;

    public HasValidSubjectIds(String expectedSubjectId) {
        this.expectedSubjectId = expectedSubjectId;
    }


    @Override
    public Result<Void> apply(VerifiableCredential credential) {
        var violatingSubIds = credential.getCredentialSubject().stream()
                .map(CredentialSubject::getId)
                .filter(id -> !expectedSubjectId.equals(id))
                .toList();
        return violatingSubIds.isEmpty() ?
                success() : failure("Not all credential subject IDs match the expected subject ID '%s'. Violating subject IDs: %s".formatted(expectedSubjectId, violatingSubIds));
    }
}
