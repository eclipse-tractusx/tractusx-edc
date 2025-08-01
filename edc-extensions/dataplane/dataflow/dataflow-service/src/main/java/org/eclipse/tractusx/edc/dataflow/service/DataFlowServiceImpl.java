/********************************************************************************
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.dataflow.service;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.tractusx.non.finite.provider.push.spi.FinitenessEvaluator;
import org.eclipse.tractusx.edc.spi.dataflow.DataFlowService;
import org.jetbrains.annotations.NotNull;

import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.STARTED;
import static org.eclipse.edc.spi.result.ServiceResult.badRequest;
import static org.eclipse.edc.spi.result.ServiceResult.conflict;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PUSH;

public class DataFlowServiceImpl implements DataFlowService {

    private final DataPlaneStore dataPlaneStore;
    private final FinitenessEvaluator finitenessEvaluator;

    public DataFlowServiceImpl(DataPlaneStore dataPlaneStore, FinitenessEvaluator finitenessEvaluator) {
        this.dataPlaneStore = dataPlaneStore;
        this.finitenessEvaluator = finitenessEvaluator;
    }

    @Override
    public @NotNull ServiceResult<Void> trigger(String dataflowId) {
        return ServiceResult.from(dataPlaneStore.findByIdAndLease(dataflowId))
                .compose(this::trigger);
    }

    private ServiceResult<Void> trigger(DataFlow dataflow) {
        if (!isPushFlowType(dataflow)) {
            return badRequest("Could not trigger dataflow %s because it's not PUSH flow type"
                    .formatted(dataflow.getId()));
        }

        if (!finitenessEvaluator.isNonFinite(dataflow)) {
            return badRequest("Could not trigger dataflow %s because underlying asset is finite"
                    .formatted(dataflow.getId()));
        }

        if (!isInStartedState(dataflow)) {
            return conflict("Could not trigger dataflow %s because it's not STARTED. Current state is %s"
                    .formatted(dataflow.getId(), dataflow.stateAsString()));
        }

        dataflow.transitToReceived(dataflow.getRuntimeId());
        dataPlaneStore.save(dataflow);
        return ServiceResult.success();
    }

    private boolean isPushFlowType(DataFlow dataflow) {
        return PUSH.equals(dataflow.getTransferType().flowType());
    }

    private boolean isInStartedState(DataFlow dataflow) {
        return STARTED.name().equals(dataflow.stateAsString());
    }

}
