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
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.mock.ResponseQueue;

import java.util.List;

/**
 * Stub implementation of the {@link ContractAgreementService} for testing purposes.
 *
 * @deprecated since 0.11.0
 */
@Deprecated(since = "0.11.0")
public class ContractAgreementServiceStub extends AbstractServiceStub implements ContractAgreementService {

    public ContractAgreementServiceStub(ResponseQueue responseQueue) {
        super(responseQueue);
    }

    @Override
    public ContractAgreement findById(String contractAgreementId) {
        return responseQueue.getNext(ContractAgreement.class, "Error finding ContractAgreement: %s")
                .orElseThrow(InvalidRequestException::new);
    }

    @Override
    public ServiceResult<List<ContractAgreement>> search(QuerySpec query) {
        return responseQueue.getNextAsList(ContractAgreement.class, "Error searching ContractAgreement: %s");
    }

    @Override
    public ContractNegotiation findNegotiation(String contractAgreementId) {
        return responseQueue.getNext(ContractNegotiation.class, "Error finding ContractNegotiation: %s")
                .orElseThrow(InvalidRequestException::new);
    }
}
