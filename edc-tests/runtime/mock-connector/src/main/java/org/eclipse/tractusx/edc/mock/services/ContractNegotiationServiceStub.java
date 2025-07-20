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

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.mock.ResponseQueue;

import java.util.List;

public class ContractNegotiationServiceStub extends AbstractServiceStub implements ContractNegotiationService {
    public ContractNegotiationServiceStub(ResponseQueue responseQueue) {
        super(responseQueue);
    }

    @Override
    public ContractNegotiation findbyId(String contractNegotiationId) {
        return responseQueue.getNext(ContractNegotiation.class, "Error finding ContractNegotiation: %s")
                .orElseThrow(InvalidRequestException::new);
    }

    @Override
    public ServiceResult<List<ContractNegotiation>> search(QuerySpec query) {
        return responseQueue.getNextAsList(ContractNegotiation.class, "Error searching for ContractNegotiation: %s");
    }

    @Override
    public String getState(String negotiationId) {
        return responseQueue.getNext(String.class, "Error getting state of ContractNegotiation: %s")
                .orElseThrow(InvalidRequestException::new);
    }

    @Override
    public ContractAgreement getForNegotiation(String negotiationId) {
        return responseQueue.getNext(ContractAgreement.class, "Error getting ContractAgreement: %s")
                .orElseThrow(InvalidRequestException::new);
    }

    @Override
    public ContractNegotiation initiateNegotiation(ContractRequest request) {
        return responseQueue.getNext(ContractNegotiation.class, "Error initiating ContractNegotiation: %s")
                .orElseThrow(InvalidRequestException::new);
    }

    @Override
    public ServiceResult<Void> terminate(TerminateNegotiationCommand command) {
        return responseQueue.getNext(Void.class, "Error terminating ContractAgreement: %s");
    }

    @Override
    public ServiceResult<Void> delete(String negotiationId) {
        return responseQueue.getNext(Void.class, "Error deleting ContractAgreement: %s");
    }
}
