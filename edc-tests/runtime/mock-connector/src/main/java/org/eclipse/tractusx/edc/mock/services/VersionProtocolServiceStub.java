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

import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersions;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionProtocolService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.tractusx.edc.mock.ResponseQueue;

public class VersionProtocolServiceStub extends AbstractServiceStub implements VersionProtocolService {

    public VersionProtocolServiceStub(ResponseQueue responseQueue) {
        super(responseQueue);
    }

    @Override
    public ServiceResult<ProtocolVersions> getAll(TokenRepresentation tokenRepresentation) {
        return responseQueue.getNext(ProtocolVersions.class, "Error retrieving ProtocolVersions: %s");
    }
}
