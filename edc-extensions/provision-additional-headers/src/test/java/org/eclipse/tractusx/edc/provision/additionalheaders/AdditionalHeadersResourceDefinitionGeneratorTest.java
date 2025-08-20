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
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdditionalHeadersResourceDefinitionGeneratorTest {

    private final ContractAgreementService contractAgreementService = mock();
    private final BdrsClient bdrsClient = mock();
    private final ProviderResourceDefinitionGenerator generator = new AdditionalHeadersResourceDefinitionGenerator(contractAgreementService, bdrsClient);

    private static ContractAgreement contractAgreementWithConsumerId(String bpn) {
        return ContractAgreement.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .consumerId(bpn)
                .providerId("providerId")
                .assetId("assetId")
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    @Test
    void canGenerate_shouldReturnFalseForNotHttpDataAddresses() {
        var dataAddress = DataAddress.Builder.newInstance().type("any").build();
        var build = Policy.Builder.newInstance().build();
        var transferProcess = TransferProcess.Builder.newInstance().build();

        var result = generator.canGenerate(transferProcess, dataAddress, build);

        assertThat(result).isFalse();
    }

    @Test
    void canGenerate_shouldReturnTrueForHttpDataAddresses() {
        var dataAddress = DataAddress.Builder.newInstance().type("HttpData").build();
        var build = Policy.Builder.newInstance().build();
        var transferProcess = TransferProcess.Builder.newInstance().build();

        var result = generator.canGenerate(transferProcess, dataAddress, build);

        assertThat(result).isTrue();
    }

    @Test
    void shouldCreateResourceDefinitionWithDataAddress() {
        var dataAddress = HttpDataAddress.Builder.newInstance().baseUrl("http://any").build();
        var build = Policy.Builder.newInstance().build();
        when(contractAgreementService.findById(any())).thenReturn(contractAgreementWithConsumerId("bpn"));
        var transferProcess = TransferProcess.Builder.newInstance()
                .dataDestination(dataAddress)
                .contractId("contractId")
                .build();

        var result = generator.generate(transferProcess, dataAddress, build);

        assertThat(result)
                .asInstanceOf(type(AdditionalHeadersResourceDefinition.class))
                .satisfies(resourceDefinition -> {
                    assertThat(resourceDefinition.getDataAddress())
                            .extracting(address -> HttpDataAddress.Builder.newInstance().copyFrom(address).build())
                            .extracting(HttpDataAddress::getBaseUrl)
                            .isEqualTo("http://any");
                    assertThat(resourceDefinition.getContractId()).isEqualTo("contractId");
                    assertThat(resourceDefinition.getBpn()).isEqualTo("bpn");
                });
        verify(contractAgreementService).findById("contractId");
    }
    
    @Test
    void whenIdIsDid_shouldCallBdrsClientAndCreateResourceDefinitionWithDataAddress() {
        var bpn = "bpn";
        var did = "did:web:abc";
        
        var dataAddress = HttpDataAddress.Builder.newInstance().baseUrl("http://any").build();
        var build = Policy.Builder.newInstance().build();
        when(contractAgreementService.findById(any())).thenReturn(contractAgreementWithConsumerId(did));
        when(bdrsClient.resolveBpn(did)).thenReturn(bpn);
        var transferProcess = TransferProcess.Builder.newInstance()
                .dataDestination(dataAddress)
                .contractId("contractId")
                .build();
        
        var result = generator.generate(transferProcess, dataAddress, build);
        
        assertThat(result)
                .asInstanceOf(type(AdditionalHeadersResourceDefinition.class))
                .satisfies(resourceDefinition -> {
                    assertThat(resourceDefinition.getDataAddress())
                            .extracting(address -> HttpDataAddress.Builder.newInstance().copyFrom(address).build())
                            .extracting(HttpDataAddress::getBaseUrl)
                            .isEqualTo("http://any");
                    assertThat(resourceDefinition.getContractId()).isEqualTo("contractId");
                    assertThat(resourceDefinition.getBpn()).isEqualTo(bpn);
                });
        verify(contractAgreementService).findById("contractId");
    }
}
