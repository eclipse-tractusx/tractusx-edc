/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

import java.util.Map;

import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.AUDIENCE_PROPERTY;

/**
 * Extension of {@link DataFlowPropertiesProvider} which provides additional properties in the {@link DataFlowStartMessage}
 * like the DID of the counter-party BPN. The resolution is made with the {@link BdrsClient}
 */
public class TxDataFlowPropertiesProvider implements DataFlowPropertiesProvider {

    private final BdrsClient bdrsClient;

    public TxDataFlowPropertiesProvider(BdrsClient bdrsClient) {
        this.bdrsClient = bdrsClient;
    }

    @Override
    public StatusResult<Map<String, String>> propertiesFor(TransferProcess transferProcess, Policy policy) {
        try {
            var did = bdrsClient.resolve(policy.getAssignee());
            if (did == null) {
                return StatusResult.failure(FATAL_ERROR, "Failed to fetch did for BPN %s".formatted(policy.getAssignee()));
            }
            return StatusResult.success(Map.of(AUDIENCE_PROPERTY, did));
        } catch (Exception e) {
            return StatusResult.failure(FATAL_ERROR, "Failed to fetch did for BPN %s: %s".formatted(policy.getAssignee(), e.getMessage()));
        }
    }
}
