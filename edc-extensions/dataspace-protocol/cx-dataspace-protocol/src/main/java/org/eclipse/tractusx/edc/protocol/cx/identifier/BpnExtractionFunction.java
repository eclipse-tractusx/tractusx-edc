/********************************************************************************
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

package org.eclipse.tractusx.edc.protocol.cx.identifier;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.protocol.lib.MembershipCredentialIdExtractionFunction;

import java.util.Map;
import java.util.Optional;

/**
 * Extracts the BPN (= holderIdentifier property) from the MembershipCredential as the participant id.
 * Used to handle id extraction for DSP 0.8.
 */
public class BpnExtractionFunction extends MembershipCredentialIdExtractionFunction {
    
    private static final String IDENTITY_PROPERTY = "holderIdentifier";

    public BpnExtractionFunction(Monitor monitor) {
        super(monitor);
    }

    @Override
    public String identityProperty() {
        return IDENTITY_PROPERTY;
    }
    
    @Override
    public Optional<String> getIdentifier(VerifiableCredential vc) {
        return vc.getCredentialSubject().stream()
                .flatMap(credentialSubject -> credentialSubject.getClaims().entrySet().stream())
                .filter(entry -> entry.getKey().endsWith(IDENTITY_PROPERTY))
                .map(Map.Entry::getValue)
                .map(String.class::cast)
                .findFirst();
    }
}
