/*
 * Copyright (c) 2026 Cofinity-X GmbH
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
package org.eclipse.tractusx.edc.discovery.v4alpha.service;

import org.eclipse.tractusx.edc.discovery.v4alpha.spi.IdentifierToDidMapper;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;

public class BpnMapper implements IdentifierToDidMapper {
    private static final String BPNL_PREFIX = "bpnl";

    private final BdrsClient bdrsClient;

    public BpnMapper(BdrsClient bdrsClient) {
        this.bdrsClient = bdrsClient;
    }

    @Override
    public boolean canHandle(String identifier) {
        return identifier.toLowerCase().startsWith(BPNL_PREFIX);
    }

    @Override
    public CompletableFuture<String> mapToDid(String identifier) {
        if (canHandle(identifier)) {
            return CompletableFuture.supplyAsync(() -> {
                var did = bdrsClient.resolveDid(identifier);
                if (did != null) {
                    return did;
                } else {
                    throw new IllegalArgumentException(
                            format("Given BPNL %s not found as registered identity", identifier));
                }
            });
        }
        return CompletableFuture.failedFuture(new IllegalArgumentException(
                format("Given identifier %s is not a BPNL", identifier)));
    }
}
