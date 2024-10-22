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

import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.iam.identitytrust.sts.embedded.EmbeddedSecureTokenService;
import org.eclipse.edc.jwt.signer.spi.JwsSignerProvider;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.JwtGenerationService;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

public class SecureTokenServiceExtension implements ServiceExtension {
    public static final String STS_PRIVATE_KEY_ALIAS = "edc.iam.sts.privatekey.alias";
    public static final String STS_PUBLIC_KEY_ID = "edc.iam.sts.publickey.id";
    private static final String STS_TOKEN_EXPIRATION = "edc.iam.sts.token.expiration"; // in minutes
    private static final int DEFAULT_STS_TOKEN_EXPIRATION_MIN = 5;
    @Inject
    private PrivateKeyResolver privateKeyResolver;

    @Inject
    private Clock clock;

    @Inject
    private JwsSignerProvider jwsSignerProvider;

    @Inject
    private JtiValidationStore jtiValidationStore;

    @Provider
    public SecureTokenService createEmbeddedSts(ServiceExtensionContext context) {
        var tokenExpiration = context.getSetting(STS_TOKEN_EXPIRATION, DEFAULT_STS_TOKEN_EXPIRATION_MIN);
        var publicKeyId = context.getSetting(STS_PUBLIC_KEY_ID, null);
        var privKeyAlias = context.getSetting(STS_PRIVATE_KEY_ALIAS, null);

        return new EmbeddedSecureTokenService(new JwtGenerationService(jwsSignerProvider), () -> privKeyAlias, () -> publicKeyId, clock, TimeUnit.MINUTES.toSeconds(tokenExpiration), jtiValidationStore);
    }
}
