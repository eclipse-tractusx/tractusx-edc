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

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.mock.ResponseQueue;

import java.util.List;

public class AssetServiceStub implements AssetService {

    private final ResponseQueue responseQueue;
    private final Monitor monitor;

    public AssetServiceStub(ResponseQueue responseQueue, Monitor monitor) {
        this.responseQueue = responseQueue;
        this.monitor = monitor;
    }

    @Override
    public Asset findById(String assetId) {
        return responseQueue.getNext(Asset.class, "Error finding asset by ID: %s")
                .orElseThrow(InvalidRequestException::new);
    }

    @Override
    public ServiceResult<List<Asset>> search(QuerySpec query) {
        return responseQueue.getNextAsList(Asset.class, "Error executing asset search: %s");
    }

    @Override
    public ServiceResult<Asset> create(Asset asset) {
        return responseQueue.getNext(Asset.class, "Error executing asset creation: %s");
    }

    @Override
    public ServiceResult<Asset> delete(String assetId) {
        return responseQueue.getNext(Asset.class, "Error executing asset deletion: %s");
    }

    @Override
    public ServiceResult<Asset> update(Asset asset) {
        return responseQueue.getNext(Asset.class, "Error executing asset update: %s");
    }
}
