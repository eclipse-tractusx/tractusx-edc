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
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.mock.services.AssetServiceStub;
import org.eclipse.tractusx.edc.mock.services.ContractAgreementServiceStub;
import org.eclipse.tractusx.edc.mock.services.ContractDefinitionServiceStub;
import org.eclipse.tractusx.edc.mock.services.ContractNegotiationServiceStub;
import org.eclipse.tractusx.edc.mock.services.PolicyDefinitionServiceStub;
import org.eclipse.tractusx.edc.mock.services.TransferProcessServiceStub;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
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
            var tpQuery = typeManager.getMapper().readValue(Thread.currentThread().getContextClassLoader().getResourceAsStream("transferprocess.request.json"), RecordedRequest.class);
            recordedRequests.offer(assetQuery);
            recordedRequests.offer(assetCreation);
            recordedRequests.offer(tpQuery);

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
    public ContractAgreementService mockContractAgreementService(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix("ContractAgreementService");
        return new ContractAgreementServiceStub(new ResponseQueue(recordedRequests, monitor));
    }

    @Provider
    public ContractDefinitionService mockContractDefService(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix("ContractDefinitionService");
        return new ContractDefinitionServiceStub(new ResponseQueue(recordedRequests, monitor));
    }

    @Provider
    public ContractNegotiationService mockContractNegService(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix("ContractNegotiationService");
        return new ContractNegotiationServiceStub(new ResponseQueue(recordedRequests, monitor));
    }

    @Provider
    public PolicyDefinitionService mockPolicyDefService(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix("PolicyDefinitionService");
        return new PolicyDefinitionServiceStub(new ResponseQueue(recordedRequests, monitor), monitor);
    }

    @Provider
    public TransferProcessService mockTransferProcessService(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix("TransferProcessService");
        return new TransferProcessServiceStub(new ResponseQueue(recordedRequests, monitor));
    }

}