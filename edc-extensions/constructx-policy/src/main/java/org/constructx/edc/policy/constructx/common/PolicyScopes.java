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

package org.constructx.edc.policy.constructx.common;

import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;

/**
 * Defines standard EDC policy scopes.
 */
public interface PolicyScopes {
    /**
     * scope of catalog requests to edc
     */
    String CATALOG_REQUEST_SCOPE = "request.catalog";

    /**
     * scope of contract negotiation requests to edc
     */
    String NEGOTIATION_REQUEST_SCOPE = "request.contract.negotiation";

    /**
     * scope of transfer process requests to edc
     */
    String TRANSFER_PROCESS_REQUEST_SCOPE = "request.transfer.process";

    /**
     * Scope for assets, policies
     */
    String CATALOG_SCOPE = "catalog";

    /**
     * Scope for negotiations
     */
    String NEGOTIATION_SCOPE = "contract.negotiation";

    /**
     * Scope for transfer of data
     */
    String TRANSFER_PROCESS_SCOPE = "transfer.process";

    /**
     * Scope class for catalog
     */
    Class<CatalogPolicyContext> CATALOG_SCOPE_CLASS = CatalogPolicyContext.class;

    /**
     * Scope for contract negotiation
     */
    Class<ContractNegotiationPolicyContext> NEGOTIATION_SCOPE_CLASS = ContractNegotiationPolicyContext.class;

    /**
     * Scope for transfer process
     */
    Class<TransferProcessPolicyContext> TRANSFER_PROCESS_SCOPE_CLASS = TransferProcessPolicyContext.class;
}
