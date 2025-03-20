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

package org.eclipse.tractusx.edc.tests.runtimes;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.boot.system.DependencyGraph;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.eclipse.tractusx.edc.tests.MockBpnIdentityService;

import java.util.Map;

/**
 * Extends the {@link EmbeddedRuntime} adding a key pair to the runtime as well as adding a facility to purge the database ({@link DataWiper}).
 */
public class ParticipantRuntime extends EmbeddedRuntime {

    private final Map<String, String> properties;
    private final ECKey runtimeKeyPair;
    private DataWiper wiper;

    public ParticipantRuntime(String moduleName, String runtimeName, String bpn, Map<String, String> properties) {
        super(runtimeName, properties, moduleName);
        this.properties = properties;
        this.registerServiceMock(IdentityService.class, new MockBpnIdentityService(bpn));
        this.registerServiceMock(AudienceResolver.class, remoteMessage -> Result.success(remoteMessage.getCounterPartyAddress()));
        this.registerServiceMock(BdrsClient.class, (s) -> s);

        var kid = properties.get("edc.iam.issuer.id") + "#key-1";
        var privateAlias = properties.get("edc.transfer.proxy.token.signer.privatekey.alias");
        try {
            runtimeKeyPair = new ECKeyGenerator(Curve.P_256).keyID(kid).generate();
            KeyPool.register(kid, runtimeKeyPair.toKeyPair());
            var privateKey = runtimeKeyPair.toPrivateKey();

            //            registerServiceMock(SecureTokenService.class, new EmbeddedSecureTokenService(new JwtGenerationService(new DefaultJwsSignerProvider((k) -> Result.success(privateKey))),
            //                    () -> privateAlias,
            //                    () -> kid, Clock.systemUTC(), Duration.ofMinutes(10).toMillis(), new InMemoryJtiValidationStore()));
            registerServiceMock(DidPublicKeyResolver.class, keyId -> Result.success(KeyPool.forId(keyId).getPublic()));
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }

    }

    public DataWiper getWiper() {
        return wiper;
    }

    @Override
    protected void bootExtensions(ServiceExtensionContext context, DependencyGraph dependencyGraph) {
        super.bootExtensions(context, dependencyGraph);
        wiper = new DataWiper(context);
        registerConsumerPullKeys(runtimeKeyPair);
    }

    private void registerConsumerPullKeys(ECKey ecKey) {
        var privateAlias = properties.get("edc.transfer.proxy.token.signer.privatekey.alias");
        var publicAlias = properties.get("edc.transfer.proxy.token.verifier.publickey.alias");

        if (privateAlias != null && publicAlias != null) {
            var vault = getContext().getService(Vault.class);
            vault.storeSecret(privateAlias, ecKey.toJSONString());
            vault.storeSecret(publicAlias, ecKey.toPublicJWK().toJSONString());
        }
    }

}
