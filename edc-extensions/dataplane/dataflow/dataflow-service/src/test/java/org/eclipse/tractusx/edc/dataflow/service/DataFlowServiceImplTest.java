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
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.eclipse.edc.tractusx.non.finite.provider.push.spi.FinitenessEvaluator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.STARTED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.TERMINATED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.BAD_REQUEST;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PULL;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PUSH;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataFlowServiceImplTest {

    private static final String DATAFLOW_ID = "123";

    private final DataPlaneStore store = mock();
    private final FinitenessEvaluator finitenessEvaluator = mock();
    private final DataFlowServiceImpl service = new DataFlowServiceImpl(store, finitenessEvaluator);

    @Test
    public void trigger_shouldReturnFailure_whenDataFlowNotFound() {
        when(store.findByIdAndLease(DATAFLOW_ID)).thenReturn(StoreResult.notFound("not-found"));

        var result = service.trigger(DATAFLOW_ID);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
    }

    @Test
    public void trigger_shouldReturnFailure_whenDataFlowIsLeasedByAnotherRuntime() {
        when(store.findByIdAndLease(DATAFLOW_ID)).thenReturn(StoreResult.alreadyLeased("already-leased"));

        var result = service.trigger(DATAFLOW_ID);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
    }

    @Test
    public void trigger_shouldReturnFailure_whenDataFlowIsNotPushFlowType() {
        var pullDataFlow = DataFlow.Builder.newInstance()
                .transferType(new TransferType("destination", PULL))
                .build();

        when(store.findByIdAndLease(DATAFLOW_ID)).thenReturn(StoreResult.success(pullDataFlow));

        var expectedErrorMessage = "Could not trigger dataflow %s because it's not PUSH flow type"
                .formatted(pullDataFlow.getId());

        var result = service.trigger(DATAFLOW_ID);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        assertThat(result.getFailureDetail()).isEqualTo(expectedErrorMessage);
    }

    @Test
    public void trigger_shouldReturnFailure_whenDataFlowIsFinite() {
        var finiteDataFlow = DataFlow.Builder.newInstance()
                .transferType(new TransferType("destination", PUSH))
                .build();

        when(store.findByIdAndLease(DATAFLOW_ID)).thenReturn(StoreResult.success(finiteDataFlow));
        when(finitenessEvaluator.isNonFinite(finiteDataFlow)).thenReturn(false);

        var expectedErrorMessage = "Could not trigger dataflow %s because underlying asset is finite"
                .formatted(finiteDataFlow.getId());

        var result = service.trigger(DATAFLOW_ID);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        assertThat(result.getFailureDetail()).isEqualTo(expectedErrorMessage);
    }

    @Test
    public void trigger_shouldReturnFailure_whenDataFlowIsNotInStartedState() {
        var terminatedDataFlow = DataFlow.Builder.newInstance()
                .state(TERMINATED.code())
                .transferType(new TransferType("destination", PUSH))
                .build();

        when(store.findByIdAndLease(DATAFLOW_ID)).thenReturn(StoreResult.success(terminatedDataFlow));
        when(finitenessEvaluator.isNonFinite(terminatedDataFlow)).thenReturn(true);

        var expectedErrorMessage = "Could not trigger dataflow %s because it's not STARTED. Current state is %s"
                .formatted(terminatedDataFlow.getId(), terminatedDataFlow.stateAsString());

        var result = service.trigger(DATAFLOW_ID);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        assertThat(result.getFailureDetail()).isEqualTo(expectedErrorMessage);
    }

    @Test
    public void trigger_shouldReturnSuccess_whenAllValidationsSucceed() {
        var dataFlow = DataFlow.Builder.newInstance()
                .state(STARTED.code())
                .transferType(new TransferType("destination", PUSH))
                .build();

        when(store.findByIdAndLease(DATAFLOW_ID)).thenReturn(StoreResult.success(dataFlow));
        when(finitenessEvaluator.isNonFinite(dataFlow)).thenReturn(true);
        doNothing().when(store).save(dataFlow);

        var result = service.trigger(DATAFLOW_ID);

        assertThat(result).isSucceeded();
    }
}
