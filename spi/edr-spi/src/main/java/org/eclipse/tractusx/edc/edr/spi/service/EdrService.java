/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.edr.spi.service;

import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.tractusx.edc.edr.spi.types.NegotiateEdrRequest;

import java.util.List;

/**
 * Service for opening a transfer process.
 */
@ExtensionPoint
public interface EdrService {

    /**
     * Open a transfer process by firing a contract negotiation. Implementors should fire a contract negotiation
     * and automatically fire a transfer process once the agreement has been reached.
     *
     * @param request The open request
     * @return The result containing the contract negotiation id
     */
    ServiceResult<ContractNegotiation> initiateEdrNegotiation(NegotiateEdrRequest request);

    /**
     * Return a {@link EndpointDataReference} associated with the transferProcessId in input
     *
     * @param transferProcessId The transferProcessId
     * @return The result containing the {@link EndpointDataReference}
     */
    ServiceResult<EndpointDataReference> findByTransferProcessId(String transferProcessId);

    ServiceResult<List<EndpointDataReferenceEntry>> findBy(QuerySpec querySpec);

    ServiceResult<EndpointDataReferenceEntry> deleteByTransferProcessId(String transferProcessId);

}
