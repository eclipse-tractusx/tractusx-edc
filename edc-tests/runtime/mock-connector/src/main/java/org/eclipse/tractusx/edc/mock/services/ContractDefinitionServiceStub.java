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

package org.eclipse.tractusx.edc.mock.services;

import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.mock.ResponseQueue;

import java.util.List;

/**
 * Stub implementation of the {@link ContractDefinitionService}.
 *
 * @deprecated since 0.11.0
 */
@Deprecated(since = "0.11.0")
public class ContractDefinitionServiceStub extends AbstractServiceStub implements ContractDefinitionService {


    public ContractDefinitionServiceStub(ResponseQueue responseQueue) {
        super(responseQueue);

    }

    @Override
    public ContractDefinition findById(String contractDefinitionId) {
        return responseQueue.getNext(ContractDefinition.class, "Error finding ContractDefinition: %s")
                .orElseThrow(InvalidRequestException::new);
    }

    @Override
    public ServiceResult<List<ContractDefinition>> search(QuerySpec query) {
        return responseQueue.getNextAsList(ContractDefinition.class, "Error searching ContractDefinition: %s");
    }

    @Override
    public ServiceResult<ContractDefinition> create(ContractDefinition contractDefinition) {
        return responseQueue.getNext(ContractDefinition.class, "Error creating ContractDefinition: %s");
    }

    @Override
    public ServiceResult<Void> update(ContractDefinition contractDefinition) {
        return responseQueue.getNext(Void.class, "Error updating ContractDefinition: %s");
    }

    @Override
    public ServiceResult<ContractDefinition> delete(String contractDefinitionId) {
        return responseQueue.getNext(ContractDefinition.class, "Error deleting ContractDefinition: %s");
    }
}
