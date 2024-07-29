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

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.tractusx.edc.mock.api.instrumentation.InstrumentationApiController;
import org.eclipse.tractusx.edc.mock.services.AssetServiceStub;
import org.eclipse.tractusx.edc.mock.services.ContractAgreementServiceStub;
import org.eclipse.tractusx.edc.mock.services.ContractDefinitionServiceStub;
import org.eclipse.tractusx.edc.mock.services.ContractNegotiationServiceStub;
import org.eclipse.tractusx.edc.mock.services.PolicyDefinitionServiceStub;
import org.eclipse.tractusx.edc.mock.services.TransferProcessServiceStub;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MockServiceExtension implements ServiceExtension {
    private final Queue<RecordedRequest<?, ?>> recordedRequests = new ConcurrentLinkedQueue<>();
    @Inject
    private TypeManager typeManager;

    @Inject
    private WebService webService;

    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor().withPrefix("ResponseQueue");
        webService.registerResource(new InstrumentationApiController(new ResponseQueue(recordedRequests, monitor)));

        // register custom deserializer for the ServiceFailure
        var mapper = typeManager.getMapper();
        var module = new SimpleModule();
        module.addDeserializer(ServiceFailure.class, new ServiceFailureDeserializer());
        mapper.registerModule(module);
    }

    @Provider
    public AssetService mockAssetService(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix("ResponseQueue");
        return new AssetServiceStub(new ResponseQueue(recordedRequests, monitor));
    }

    @Provider
    public CatalogService mockCatalogService() {
        return new CatalogService() {

            @Override
            public CompletableFuture<StatusResult<byte[]>> requestCatalog(String s, String s1, String s2, QuerySpec querySpec, String... strings) {
                return null;
            }

            @Override
            public CompletableFuture<StatusResult<byte[]>> requestDataset(String s, String s1, String s2, String s3) {
                return null;
            }
        };
    }

    @Provider
    public ContractAgreementService mockContractAgreementService() {
        return new ContractAgreementServiceStub(new ResponseQueue(recordedRequests, monitor));
    }

    @Provider
    public ContractDefinitionService mockContractDefService() {
        return new ContractDefinitionServiceStub(new ResponseQueue(recordedRequests, monitor));
    }

    @Provider
    public ContractNegotiationService mockContractNegService() {
        return new ContractNegotiationServiceStub(new ResponseQueue(recordedRequests, monitor));
    }

    @Provider
    public PolicyDefinitionService mockPolicyDefService() {
        return new PolicyDefinitionServiceStub(new ResponseQueue(recordedRequests, monitor));
    }

    @Provider
    public TransferProcessService mockTransferProcessService() {
        return new TransferProcessServiceStub(new ResponseQueue(recordedRequests, monitor));
    }

}
