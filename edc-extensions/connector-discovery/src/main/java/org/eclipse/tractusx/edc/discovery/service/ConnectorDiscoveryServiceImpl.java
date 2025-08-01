/*
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
 */

package org.eclipse.tractusx.edc.discovery.service;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.tractusx.edc.discovery.models.ConnectorDiscoveryRequest;

public class ConnectorDiscoveryServiceImpl {

    public ConnectorDiscoveryServiceImpl() {

    }

    public ServiceResult<JsonArray> discover(ConnectorDiscoveryRequest request) {

        var message = Json.createArrayBuilder().add(
                Json.createObjectBuilder()
                        .add("connectors", Json.createArrayBuilder().add(
                                Json.createObjectBuilder()
                                        .add("counterPartyId", "did:web:provider")
                                        .add("protocol", "dataspace-protocol-http:2025-1")
                                        .build()
                        ).build()).build()
        ).build();

        return ServiceResult.success(message);
    }
}
