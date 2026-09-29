/*
 * Copyright (c) 2025 Think-it GmbH
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

package org.eclipse.tractusx.edc.connector;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Optional.ofNullable;

@Deprecated(since = "0.12.0") // can be removed once https://github.com/eclipse-edc/Connector/pull/5396 is merged and released
public class InMemorySingleParticipantVault implements Vault {
    private static final String DEFAULT_PARTITION = "default";
    private final Map<String, Map<String, String>> secrets = new ConcurrentHashMap<>();
    private final Monitor monitor;

    public InMemorySingleParticipantVault(Monitor monitor) {
        this.monitor = monitor.withPrefix(getClass().getSimpleName());
    }

    @Override
    public @Nullable String resolveSecret(String key) {
        return resolveSecret(DEFAULT_PARTITION, key);
    }

    @Override
    public Result<Void> storeSecret(String key, String value) {
        return storeSecret(DEFAULT_PARTITION, key, value);
    }

    @Override
    public Result<Void> deleteSecret(String key) {
        return deleteSecret(DEFAULT_PARTITION, key);
    }

    @Override
    public @Nullable String resolveSecret(String vaultPartition, String s) {
        monitor.debug("Resolving secret " + s);
        if (s == null) {
            monitor.warning("Secret name is null - skipping");
            return null;
        }
        return ofNullable(secrets.get(DEFAULT_PARTITION)).map(map -> map.getOrDefault(s, null)).orElse(null);
    }

    @Override
    public Result<Void> storeSecret(String vaultPartition, String s, String s1) {
        monitor.debug("Storing secret " + s);

        var partition = secrets.computeIfAbsent(DEFAULT_PARTITION, k -> new ConcurrentHashMap<>());
        partition.put(s, s1);
        return Result.success();
    }

    @Override
    public Result<Void> deleteSecret(String vaultPartition, String s) {
        monitor.debug("Deleting secret " + s);

        var result = ofNullable(secrets.get(DEFAULT_PARTITION)).map(map -> map.remove(s)).orElse(null);

        return result == null ?
                Result.failure("Secret with key " + s + " does not exist") :
                Result.success();
    }
}
