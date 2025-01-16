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

package org.eclipse.edc.connector.dataplane.framework.store;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.store.InMemoryStatefulEntityStore;

import java.time.Clock;
import java.util.UUID;

/**
 * Implements an in-memory, ephemeral store with a maximum capacity. If the store grows beyond capacity, the oldest entry will be evicted.
 */
public class InMemoryDataPlaneStore extends InMemoryStatefulEntityStore<DataFlow> implements DataPlaneStore {

    public InMemoryDataPlaneStore(Clock clock, CriterionOperatorRegistry criterionOperatorRegistry) {
        this(UUID.randomUUID().toString(), clock, criterionOperatorRegistry);
    }

    public InMemoryDataPlaneStore(String connectorName, Clock clock, CriterionOperatorRegistry criterionOperatorRegistry) {
        super(DataFlow.class, connectorName, clock, criterionOperatorRegistry);
    }
}
