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

package org.eclipse.tractusx.edc.agreements.retirement.service;

import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.agreements.retirement.spi.service.AgreementsRetirementService;
import org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore;

import static org.eclipse.tractusx.edc.agreements.retirement.AgreementsRetirementPreValidatorRegisterExtension.NAME;

@Extension(NAME)
public class AgreementRetirementServiceExtension implements ServiceExtension {

    private static final String NAME = "Agreement Retirement Service Extension";

    @Override
    public String name() {
        return NAME;
    }

    @Inject
    AgreementsRetirementStore store;

    @Inject
    TransactionContext transactionContext;

    @Inject
    ContractAgreementService contractAgreementService;

    @Provider()
    public AgreementsRetirementService createInMemAgreementRetirementService() {
        return new AgreementsRetirementServiceImpl(store, transactionContext, contractAgreementService);
    }
}
