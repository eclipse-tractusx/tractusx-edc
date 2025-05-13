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
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.security.token.jwt.DefaultJwsSignerProvider;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.token.spi.KeyIdDecorator;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Cleans up the database after every test to create a clean slate. This is faster than using a {@link org.eclipse.edc.junit.extensions.RuntimePerMethodExtension},
 * especially with postgres
 */
public class ParticipantRuntimeExtension extends RuntimePerClassExtension implements AfterEachCallback {

    private final AtomicReference<DataWiper> wiper = new AtomicReference<>();

    public ParticipantRuntimeExtension(EmbeddedRuntime runtime) {
        super(runtime);
        registerSystemExtension(ServiceExtension.class, new SignServicesExtension(this));
        registerSystemExtension(ServiceExtension.class, new DataWiperExtension(wiper, DataWiper::new));
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        wiper.get().clearPersistence();
    }

    public static class SignServicesExtension implements ServiceExtension {

        private final ParticipantRuntimeExtension participantRuntimeExtension;
        @Inject
        private Vault vault;

        public SignServicesExtension(ParticipantRuntimeExtension participantRuntimeExtension) {
            this.participantRuntimeExtension = participantRuntimeExtension;
        }

        @Override
        public void initialize(ServiceExtensionContext context) {
            var config = context.getConfig();
            var kid = config.getString("edc.iam.issuer.id") + "#key-1";
            var privateAlias = config.getString("edc.transfer.proxy.token.signer.privatekey.alias");
            var publicAlias = config.getString("edc.transfer.proxy.token.verifier.publickey.alias");
            try {
                var runtimeKeyPair = new ECKeyGenerator(Curve.P_256).keyID(kid).generate();
                KeyPool.register(kid, runtimeKeyPair.toKeyPair());
                var privateKey = runtimeKeyPair.toPrivateKey();

                var jwtGenerationService = new JwtGenerationService(new DefaultJwsSignerProvider(s -> Result.success(privateKey)));
                participantRuntimeExtension.registerServiceMock(SecureTokenService.class, (claims, bearerAccessScope) -> {
                    var decorator = new TokenDecorator() {
                        @Override
                        public TokenParameters.Builder decorate(TokenParameters.Builder tokenParameters) {
                            claims.forEach(tokenParameters::claims);
                            return tokenParameters;
                        }
                    };
                    return jwtGenerationService.generate(privateAlias, new KeyIdDecorator(kid), decorator);
                });

                participantRuntimeExtension.registerServiceMock(DidPublicKeyResolver.class, keyId -> Result.success(KeyPool.forId(keyId).getPublic()));

                vault.storeSecret(privateAlias, runtimeKeyPair.toJSONString());
                vault.storeSecret(publicAlias, runtimeKeyPair.toPublicJWK().toJSONString());
            } catch (JOSEException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
