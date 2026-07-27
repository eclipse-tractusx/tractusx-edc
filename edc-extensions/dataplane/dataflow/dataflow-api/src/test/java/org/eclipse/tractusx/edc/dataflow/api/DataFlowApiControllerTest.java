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

package org.eclipse.tractusx.edc.dataflow.api;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectConflictException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.tractusx.edc.spi.dataflow.DataFlowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataFlowApiControllerTest {

    private static final String DATAFLOW_ID = "123";

    private final Monitor monitor = mock();
    private final DataFlowService service = mock();

    private DataFlowApiController controller;

    @BeforeEach
    void setUp() {
        when(monitor.withPrefix(anyString())).thenReturn(monitor);
        controller = new DataFlowApiController(monitor, service);
    }

    @Test
    void trigger_shouldSucceed_whenServiceReturnsSuccess() {
        when(service.trigger(DATAFLOW_ID)).thenReturn(ServiceResult.success());

        assertThatNoException().isThrownBy(() -> controller.trigger(DATAFLOW_ID));

        verify(service).trigger(DATAFLOW_ID);
    }

    @Test
    void trigger_shouldThrowObjectNotFound_whenServiceReturnsNotFound() {
        when(service.trigger(DATAFLOW_ID)).thenReturn(ServiceResult.notFound("not-found"));

        assertThatThrownBy(() -> controller.trigger(DATAFLOW_ID)).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void trigger_shouldThrowInvalidRequest_whenServiceReturnsBadRequest() {
        when(service.trigger(DATAFLOW_ID)).thenReturn(ServiceResult.badRequest("bad-request"));

        assertThatThrownBy(() -> controller.trigger(DATAFLOW_ID)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void trigger_shouldThrowObjectConflict_whenServiceReturnsConflict() {
        when(service.trigger(DATAFLOW_ID)).thenReturn(ServiceResult.conflict("conflict"));

        assertThatThrownBy(() -> controller.trigger(DATAFLOW_ID)).isInstanceOf(ObjectConflictException.class);
    }

}

