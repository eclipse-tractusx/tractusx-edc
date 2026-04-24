/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.flow;

import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowPropertiesProvider;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.tractusx.edc.agreements.bpns.spi.store.AgreementsBpnsStore;

import java.util.Map;

import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.AUDIENCE_PROPERTY;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.BPN_PROPERTY;

/**
 * Extension of {@link DataFlowPropertiesProvider} which provides additional properties in the {@link DataFlowStartMessage}
 * such as the consumer DID (audience) and the consumer BPN. Both values are resolved locally without a BDRS call:
 * the DID comes directly from {@code policy.getAssignee()} and the BPN is looked up from {@link AgreementsBpnsStore}.
 */
public class TxDataFlowPropertiesProvider implements DataFlowPropertiesProvider {

    private final AgreementsBpnsStore agreementsBpnsStore;

    public TxDataFlowPropertiesProvider(AgreementsBpnsStore agreementsBpnsStore) {
        this.agreementsBpnsStore = agreementsBpnsStore;
    }

    @Override
    public StatusResult<Map<String, String>> propertiesFor(TransferProcess transferProcess, Policy policy) {
        var entry = agreementsBpnsStore.findByAgreementId(transferProcess.getContractId());
        if (entry == null) {
            return StatusResult.failure(FATAL_ERROR,
                    "No BPN entry found for agreement %s".formatted(transferProcess.getContractId()));
        }
        return StatusResult.success(Map.of(
                AUDIENCE_PROPERTY, policy.getAssignee(),
                BPN_PROPERTY, entry.getConsumerBpn()
        ));
    }
}
