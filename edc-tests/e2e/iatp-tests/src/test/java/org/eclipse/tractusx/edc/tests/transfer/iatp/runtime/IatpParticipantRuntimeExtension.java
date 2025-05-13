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

package org.eclipse.tractusx.edc.tests.transfer.iatp.runtime;

import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.tests.runtimes.DataWiper;
import org.eclipse.tractusx.edc.tests.runtimes.DataWiperExtension;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.security.KeyPair;
import java.util.concurrent.atomic.AtomicReference;

public class IatpParticipantRuntimeExtension extends RuntimePerClassExtension implements AfterEachCallback {

    private final AtomicReference<DataWiper> wiper = new AtomicReference<>();

    public IatpParticipantRuntimeExtension(EmbeddedRuntime runtime, KeyPair keyPair) {
        super(runtime);
        registerSystemExtension(ServiceExtension.class, new ServiceExtension() {

            @Inject
            private Vault vault;

            @Override
            public void initialize(ServiceExtensionContext context) {
                var runtimeKeyPair = CryptoConverter.createJwk(keyPair);

                var config = context.getConfig();
                var privateAlias = config.getString("edc.transfer.proxy.token.signer.privatekey.alias");
                var publicAlias = config.getString("edc.transfer.proxy.token.verifier.publickey.alias");
                vault.storeSecret(privateAlias, runtimeKeyPair.toJSONString());
                vault.storeSecret(publicAlias, runtimeKeyPair.toPublicJWK().toJSONString());
            }
        });
        registerSystemExtension(ServiceExtension.class, new DataWiperExtension(wiper, CredentialWiper::new));
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        wiper.get().clearPersistence();
    }
}
