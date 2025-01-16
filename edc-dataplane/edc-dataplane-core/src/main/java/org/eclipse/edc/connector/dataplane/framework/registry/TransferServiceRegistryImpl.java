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
import org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Default {@link TransferServiceRegistry} implementation.
 */
public class TransferServiceRegistryImpl implements TransferServiceRegistry {

    private final Collection<TransferService> transferServices = new LinkedHashSet<>();
    private final TransferServiceSelectionStrategy transferServiceSelectionStrategy;

    public TransferServiceRegistryImpl(TransferServiceSelectionStrategy transferServiceSelectionStrategy) {
        this.transferServiceSelectionStrategy = transferServiceSelectionStrategy;
    }

    @Override
    public void registerTransferService(TransferService transferService) {
        transferServices.add(transferService);
    }

    @Override
    @Nullable
    public TransferService resolveTransferService(DataFlowStartMessage request) {
        var possibleServices = transferServices.stream().filter(s -> s.canHandle(request));
        return transferServiceSelectionStrategy.chooseTransferService(request, possibleServices);
    }
}
