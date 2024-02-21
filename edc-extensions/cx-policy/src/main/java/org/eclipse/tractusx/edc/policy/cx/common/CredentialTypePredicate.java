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

package org.eclipse.tractusx.edc.policy.cx.common;

import org.eclipse.edc.identitytrust.model.VerifiableCredential;

import java.util.function.Predicate;

public class CredentialTypePredicate implements Predicate<VerifiableCredential> {
    private final String expectedType;

    public CredentialTypePredicate(String expectedType) {
        this.expectedType = expectedType;
    }

    @Override
    public boolean test(VerifiableCredential credential) {
        return credential.getType().contains(expectedType);
    }
}
