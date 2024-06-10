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

package org.eclipse.tractusx.edc.lifecycle;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Map;

public class ParticipantRuntime extends RuntimePerMethodExtension {

    private final Map<String, String> properties;

    public ParticipantRuntime(String runtimeName, Map<String, String> properties, String... modules) {
        super(new EmbeddedRuntime(runtimeName, properties, modules));
        this.properties = properties;
    }


    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) {
        registerConsumerPullKeys(properties);
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        var wiper = new DataWiper(runtime.getContext());
        wiper.clearPersistence();
    }

    private void registerConsumerPullKeys(Map<String, String> properties) {
        var privateAlias = properties.get("edc.transfer.proxy.token.signer.privatekey.alias");
        var publicAlias = properties.get("edc.transfer.proxy.token.verifier.publickey.alias");
        if (privateAlias != null && publicAlias != null) {
            try {
                var ecKey = new ECKeyGenerator(Curve.P_256).keyID(publicAlias).generate();
                var vault = getService(Vault.class);
                vault.storeSecret(privateAlias, ecKey.toJSONString());
                vault.storeSecret(publicAlias, ecKey.toPublicJWK().toJSONString());
            } catch (JOSEException e) {
                throw new RuntimeException(e);
            }

        }
    }

}
