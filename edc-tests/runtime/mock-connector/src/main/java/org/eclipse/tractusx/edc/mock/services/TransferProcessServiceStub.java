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

import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.CompleteProvisionCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.ResumeTransferCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.SuspendTransferCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.TerminateTransferCommand;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.mock.ResponseQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TransferProcessServiceStub extends AbstractServiceStub implements TransferProcessService {

    public TransferProcessServiceStub(ResponseQueue responseQueue) {
        super(responseQueue);
    }

    @Override
    public @Nullable TransferProcess findById(String transferProcessId) {
        return responseQueue.getNext(TransferProcess.class, "Error finding TransferProcess: %s").orElseThrow(f -> new InvalidRequestException(f.getFailureDetail()));
    }

    @Override
    public ServiceResult<List<TransferProcess>> search(QuerySpec query) {
        return responseQueue.getNextAsList(TransferProcess.class, "Error executing TransferProcess search: %s");
    }

    @Override
    public @Nullable String getState(String transferProcessId) {
        return responseQueue.getNext(String.class, "Error obtaining TransferProcess status: %s")
                .orElseThrow(InvalidRequestException::new);
    }

    @Override
    public @NotNull ServiceResult<Void> complete(String transferProcessId) {
        return responseQueue.getNext(Void.class, "Error completing TransferProcess: %s");
    }

    @Override
    public @NotNull ServiceResult<Void> terminate(TerminateTransferCommand command) {
        return responseQueue.getNext(Void.class, "Error terminating TransferProcess: %s");
    }

    @Override
    public @NotNull ServiceResult<Void> suspend(SuspendTransferCommand command) {
        return responseQueue.getNext(Void.class, "Error suspending TransferProcess: %s");
    }

    @Override
    public @NotNull ServiceResult<Void> resume(ResumeTransferCommand command) {
        return responseQueue.getNext(Void.class, "Error resuming TransferProcess: %s");
    }

    @Override
    public @NotNull ServiceResult<Void> deprovision(String transferProcessId) {
        return responseQueue.getNext(Void.class, "Error deprovisioning TransferProcess: %s");
    }

    @Override
    public @NotNull ServiceResult<TransferProcess> initiateTransfer(TransferRequest request) {
        return responseQueue.getNext(TransferProcess.class, "Error initiating TransferProcess: %s");
    }

    @Override
    public ServiceResult<Void> completeProvision(CompleteProvisionCommand completeProvisionCommand) {
        return responseQueue.getNext(Void.class, "Error completing provisioning");
    }

    @Override
    public ServiceResult<Void> completeDeprovision(String transferProcessId, DeprovisionedResource resource) {
        return responseQueue.getNext(Void.class, "Error completing/deprovisioning TransferProcess: %s");
    }

    @Override
    public ServiceResult<Void> addProvisionedResource(String transferProcessId, ProvisionResponse response) {
        return responseQueue.getNext(Void.class, "Error adding Provisioned resource to TransferProcess: %s");
    }
}
