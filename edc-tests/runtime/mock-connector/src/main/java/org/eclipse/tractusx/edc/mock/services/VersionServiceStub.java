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

import org.eclipse.edc.connector.controlplane.protocolversion.spi.ProtocolVersionRequest;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionService;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.tractusx.edc.mock.ResponseQueue;

import java.util.concurrent.CompletableFuture;

public class VersionServiceStub extends AbstractServiceStub implements VersionService {

    public VersionServiceStub(ResponseQueue responseQueue) {
        super(responseQueue);
    }

    @Override
    public CompletableFuture<StatusResult<byte[]>> requestVersions(ProtocolVersionRequest request) {
        var nextInQueue = responseQueue.getNext(byte[].class, "Error retrieving VersionService status result: %s");
        var result = StatusResult.success(nextInQueue.getContent());
        var resultHardcoded = StatusResult.success(new byte[]{ 91, 123, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 34, 118, 101, 114, 115, 105, 111, 110, 34, 58, 32, 34, 50, 48, 50, 52, 47, 49, 34, 44, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 34, 112, 97, 116, 104, 34, 58, 32, 34, 47, 50, 48, 50, 52, 47, 49, 34, 10, 32, 32, 32, 32, 32, 32, 32, 32, 125, 93, 10 });
        return CompletableFuture.completedFuture(resultHardcoded);
    }
}
