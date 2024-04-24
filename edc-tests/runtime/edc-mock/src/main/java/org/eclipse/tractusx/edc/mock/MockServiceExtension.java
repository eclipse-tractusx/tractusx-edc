/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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
 */

package org.eclipse.tractusx.edc.mock;

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.mock.services.AssetServiceStub;
import org.eclipse.tractusx.edc.mock.services.PolicyDefinitionServiceStub;
import org.eclipse.tractusx.edc.mock.services.TransferProcessServiceStub;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MockServiceExtension implements ServiceExtension {
    private final Deque<RecordedRequest<?, ?>> recordedRequests = new ArrayDeque<>();
    @Inject
    private TypeManager typeManager;

    @Override
    public void initialize(ServiceExtensionContext context) {
        try {
            var assetQuery = typeManager.getMapper().readValue(Thread.currentThread().getContextClassLoader().getResourceAsStream("assetquery.request.json"), RecordedRequest.class);
            var assetCreation = typeManager.getMapper().readValue(Thread.currentThread().getContextClassLoader().getResourceAsStream("assetcreation.request.json"), RecordedRequest.class);
            recordedRequests.offer(assetQuery);
            recordedRequests.offer(assetCreation);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Provider
    public AssetService mockAssetService(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix("AssetService");
        return new AssetServiceStub(new ResponseQueue(recordedRequests, monitor), monitor);
    }

    @Provider
    public CatalogService mockCatalogService() {
        return new CatalogService() {
            @Override
            public CompletableFuture<StatusResult<byte[]>> requestCatalog(String counterPartyId, String counterPartyAddress, String protocol, QuerySpec querySpec) {
                return null;
            }

            @Override
            public CompletableFuture<StatusResult<byte[]>> requestDataset(String id, String counterPartyId, String counterPartyAddress, String protocol) {
                return null;
            }
        };
    }

    @Provider
    public ContractAgreementService mockContractAgreementService() {
        return new ContractAgreementService() {
            @Override
            public ContractAgreement findById(String contractAgreementId) {
                return null;
            }

            @Override
            public ServiceResult<List<ContractAgreement>> search(QuerySpec query) {
                return null;
            }

            @Override
            public ContractNegotiation findNegotiation(String contractAgreementId) {
                return null;
            }
        };
    }

    @Provider
    public ContractDefinitionService mockContractDefService() {
        return new ContractDefinitionService() {

            @Override
            public ContractDefinition findById(String contractDefinitionId) {
                return null;
            }

            @Override
            public ServiceResult<List<ContractDefinition>> search(QuerySpec query) {
                return null;
            }

            @Override
            public ServiceResult<ContractDefinition> create(ContractDefinition contractDefinition) {
                return null;
            }

            @Override
            public ServiceResult<Void> update(ContractDefinition contractDefinition) {
                return null;
            }

            @Override
            public ServiceResult<ContractDefinition> delete(String contractDefinitionId) {
                return null;
            }
        };
    }

    @Provider
    public ContractNegotiationService mockContractNegService() {
        return new ContractNegotiationService() {
            @Override
            public ContractNegotiation findbyId(String contractNegotiationId) {
                return null;
            }

            @Override
            public ServiceResult<List<ContractNegotiation>> search(QuerySpec query) {
                return null;
            }

            @Override
            public String getState(String negotiationId) {
                return "";
            }

            @Override
            public ContractAgreement getForNegotiation(String negotiationId) {
                return null;
            }

            @Override
            public ContractNegotiation initiateNegotiation(ContractRequest request) {
                return null;
            }

            @Override
            public ServiceResult<Void> terminate(TerminateNegotiationCommand command) {
                return null;
            }
        };
    }

    @Provider
    public PolicyDefinitionService mockPolicyDefService(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix("PolicyDefinitionService");
        return new PolicyDefinitionServiceStub(new ResponseQueue(recordedRequests, monitor), monitor);
    }

    @Provider
    public TransferProcessService mockTransferProcessService(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix("PolicyDefinitionService");
        return new TransferProcessServiceStub(new ResponseQueue(recordedRequests, monitor));
    }

}