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

import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.mock.ResponseQueue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PolicyDefinitionServiceStub implements PolicyDefinitionService {
    private final ResponseQueue responseQueue;

    public PolicyDefinitionServiceStub(ResponseQueue responseQueue, Monitor monitor) {
        this.responseQueue = responseQueue;
    }

    @Override
    public PolicyDefinition findById(String policyId) {
        return responseQueue.getNext(PolicyDefinition.class, "Error finding PolicyDefinition by id: %s")
                .orElseThrow(InvalidRequestException::new);
    }

    @Override
    public ServiceResult<List<PolicyDefinition>> search(QuerySpec query) {
        return responseQueue.getNextAsList(PolicyDefinition.class, "Error executing PolicyDefinition search: %s");
    }

    @Override
    public @NotNull ServiceResult<PolicyDefinition> deleteById(String policyId) {
        return responseQueue.getNext(PolicyDefinition.class, "Error deleting PolicyDefinition: %s");
    }

    @Override
    public @NotNull ServiceResult<PolicyDefinition> create(PolicyDefinition policy) {
        return responseQueue.getNext(PolicyDefinition.class, "Error creating PolicyDefinition: %s");
    }

    @Override
    public ServiceResult<PolicyDefinition> update(PolicyDefinition policy) {
        return responseQueue.getNext(PolicyDefinition.class, "Error updating PolicyDefinition: %s");
    }


}
