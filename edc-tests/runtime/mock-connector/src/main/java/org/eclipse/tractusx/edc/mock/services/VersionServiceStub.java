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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.controlplane.protocolversion.spi.ProtocolVersionRequest;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionService;
import org.eclipse.edc.protocol.spi.ProtocolVersions;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.tractusx.edc.mock.ResponseQueue;

import java.util.concurrent.CompletableFuture;

public class VersionServiceStub extends AbstractServiceStub implements VersionService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public VersionServiceStub(ResponseQueue responseQueue) {
        super(responseQueue);
    }

    @Override
    public CompletableFuture<StatusResult<byte[]>> requestVersions(ProtocolVersionRequest request) {
        var nextInQueue = responseQueue.getNext(ProtocolVersions.class, "Error retrieving VersionService status result: %s");
        try {
            var protocolVersions = objectMapper.writeValueAsString(nextInQueue.getContent());
            var result = StatusResult.success(protocolVersions.getBytes());
            return CompletableFuture.completedFuture(result);
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
    }
}
