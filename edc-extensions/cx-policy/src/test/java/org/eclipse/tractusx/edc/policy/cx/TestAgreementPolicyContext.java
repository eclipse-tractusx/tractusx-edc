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

package org.eclipse.tractusx.edc.policy.cx;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.AgreementPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.model.Policy;

import java.time.Instant;
import java.util.UUID;

public class TestAgreementPolicyContext extends PolicyContextImpl implements AgreementPolicyContext {

    private final Instant now;
    private final ContractAgreement contractAgreement;

    public TestAgreementPolicyContext() {
        this(Instant.now());
    }

    public TestAgreementPolicyContext(Instant now) {
        this(now, createAgreement(now));
    }

    public TestAgreementPolicyContext(Instant now, ContractAgreement contractAgreement) {
        this.now = now;
        this.contractAgreement = contractAgreement;
    }

    public static ContractAgreement createAgreement(Instant signingTime) {
        return ContractAgreement.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .providerId(UUID.randomUUID().toString())
                .consumerId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .contractSigningDate(signingTime.getEpochSecond())
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    @Override
    public ContractAgreement contractAgreement() {
        return contractAgreement;
    }

    @Override
    public Instant now() {
        return now;
    }

    @Override
    public String scope() {
        return "any";
    }
}