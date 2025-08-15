/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.provision.additionalheaders;

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ProviderResourceDefinitionGenerator;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

import static org.eclipse.tractusx.edc.spi.identity.mapper.BdrsConstants.DID_PREFIX;

class AdditionalHeadersResourceDefinitionGenerator implements ProviderResourceDefinitionGenerator {

    private final ContractAgreementService contractAgreementService;
    private final BdrsClient bdrsClient;

    AdditionalHeadersResourceDefinitionGenerator(ContractAgreementService contractAgreementService, BdrsClient bdrsClient) {
        this.contractAgreementService = contractAgreementService;
        this.bdrsClient = bdrsClient;
    }

    @Override
    public @Nullable ResourceDefinition generate(TransferProcess transferProcess, DataAddress dataAddress, Policy policy) {
        var identity = Optional.of(transferProcess.getContractId())
                .map(contractAgreementService::findById)
                .map(ContractAgreement::getConsumerId)
                .orElse(null);
        
        if (identity != null && identity.startsWith(DID_PREFIX)) {
            identity = bdrsClient.resolveBpn(identity);
        }
        
        return AdditionalHeadersResourceDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .dataAddress(dataAddress)
                .contractId(transferProcess.getContractId())
                .bpn(identity)
                .build();
    }

    @Override
    public boolean canGenerate(TransferProcess transferProcess, DataAddress dataAddress, Policy policy) {
        return "HttpData".equals(dataAddress.getType());
    }
}
