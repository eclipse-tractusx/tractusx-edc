/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
 * Copyright (c) 2026 SAP SE
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

package org.eclipse.tractusx.edc.protocol.core;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Optional;

/**
 * Extracts the DID (= credential subject id) from the MembershipCredential as the participant id.
 * Used to handle id extraction for DSP 2025-1.
 */
public class DidExtractionFunction extends MembershipCredentialIdExtractionFunction {
    
    private static final String IDENTITY_PROPERTY = "id";

    public DidExtractionFunction(Monitor monitor) {
        super(monitor);
    }

    @Override
    public String identityProperty() {
        return IDENTITY_PROPERTY;
    }
    
    @Override
    public Optional<String> getIdentifier(VerifiableCredential vc) {
        return vc.getCredentialSubject().stream()
                .map(CredentialSubject::getId)
                .findFirst();
    }
}
