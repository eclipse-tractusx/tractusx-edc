/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.tests.transfer.iatp.runtime;

import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.tractusx.edc.tests.extension.VaultSeedExtension;
import org.eclipse.tractusx.edc.tests.runtimes.KeyPool;

import java.security.KeyPair;
import java.util.Map;
import java.util.function.Supplier;

public interface Runtimes {

    static RuntimeExtension dimRuntime(String name, KeyPair keyPair, Supplier<Config> configurationProvider) {
        return genericRuntime(name, ":edc-tests:runtime:iatp:runtime-memory-iatp-dim-ih", keyPair, configurationProvider)
                .registerSystemExtension(ServiceExtension.class, new VaultSeedExtension(Map.of("client_secret_alias", "client_secret")));
    }

    static RuntimeExtension iatpRuntime(String name, KeyPair keyPair, Supplier<Config> configurationProvider) {
        return genericRuntime(name, ":edc-tests:runtime:iatp:runtime-memory-iatp-ih", keyPair, configurationProvider)
                .registerSystemExtension(ServiceExtension.class, new VaultSeedExtension(Map.of("client_secret_alias", "client_secret")));
    }

    static RuntimeExtension stsRuntime(String name, KeyPair keyPair, Supplier<Config> configurationProvider) {
        return new RuntimePerClassExtension(new EmbeddedRuntime(name, ":edc-tests:runtime:iatp:runtime-memory-sts").configurationProvider(configurationProvider)
                .registerSystemExtension(ServiceExtension.class, new VaultSeedExtension(Map.of("client_secret_alias", "client_secret"))))
                .registerServiceMock(DidPublicKeyResolver.class, keyId -> Result.success(KeyPool.forId(keyId).getPublic()));
    }

    private static RuntimeExtension genericRuntime(String name, String moduleName, KeyPair keyPair, Supplier<Config> configurationProvider) {
        return new IatpParticipantRuntimeExtension(
                new EmbeddedRuntime(name, moduleName).configurationProvider(configurationProvider),
                keyPair
        ).registerServiceMock(DidPublicKeyResolver.class, keyId -> Result.success(KeyPool.forId(keyId).getPublic()));
    }

}
