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

package org.eclipse.edc.connector.dataplane.framework.registry;

import org.eclipse.edc.connector.dataplane.spi.pipeline.TransferService;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Functional interface for selecting which of (potentially) multiple {@link TransferService}s to use
 * for serving a particular {@link DataFlowStartMessage}.
 */
public interface TransferServiceSelectionStrategy {
    /**
     * Selects which of (potentially) multiple {@link TransferService}s to use
     * for serving a particular {@link DataFlowStartMessage}.
     *
     * @param request          the request.
     * @param transferServices any number of services which are able to handle the request. May be an empty {@link Stream}.
     * @return the service to be used to serve the request, selected among the input {@code transferServices}, or {@code null} if the stream is empty or no service should be used.
     */
    @Nullable
    TransferService chooseTransferService(DataFlowStartMessage request, Stream<TransferService> transferServices);

    /**
     * Default strategy: use first matching service. This allows integrators to select
     * order preferred {@link TransferService}s in the classpath.
     *
     * @return the first service, or {@code null} if the stream is empty.
     */
    static TransferServiceSelectionStrategy selectFirst() {
        return (request, transferServices) -> transferServices.findFirst().orElse(null);
    }
}
