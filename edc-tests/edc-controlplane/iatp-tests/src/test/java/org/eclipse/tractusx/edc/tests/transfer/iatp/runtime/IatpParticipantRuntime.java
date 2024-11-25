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

import com.nimbusds.jose.jwk.JWK;
import org.eclipse.edc.boot.system.DependencyGraph;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.eclipse.tractusx.edc.tests.runtimes.DataWiper;

import java.security.KeyPair;
import java.util.Map;


public class IatpParticipantRuntime extends EmbeddedRuntime {
    private final Map<String, String> properties;
    private final JWK runtimeKeyPair;
    private DataWiper wiper;

    public IatpParticipantRuntime(String moduleName, String runtimeName, Map<String, String> properties, KeyPair runtimeKeypair) {
        super(runtimeName, properties, moduleName);
        this.properties = properties;
        runtimeKeyPair = CryptoConverter.createJwk(runtimeKeypair);
        this.registerServiceMock(BdrsClient.class, (s) -> s);
    }

    public DataWiper getWiper() {
        return wiper;
    }


    @Override
    protected void bootExtensions(ServiceExtensionContext context, DependencyGraph dependencyGraph) {
        super.bootExtensions(context, dependencyGraph);
        wiper = new CredentialWiper(getContext());
        registerConsumerPullKeys(runtimeKeyPair);
    }

    private void registerConsumerPullKeys(JWK ecKey) {
        var privateAlias = properties.get("edc.transfer.proxy.token.signer.privatekey.alias");
        var publicAlias = properties.get("edc.transfer.proxy.token.verifier.publickey.alias");

        if (privateAlias != null && publicAlias != null) {
            var vault = getService(Vault.class);
            vault.storeSecret(privateAlias, ecKey.toJSONString());
            vault.storeSecret(publicAlias, ecKey.toPublicJWK().toJSONString());
        }
    }

}
