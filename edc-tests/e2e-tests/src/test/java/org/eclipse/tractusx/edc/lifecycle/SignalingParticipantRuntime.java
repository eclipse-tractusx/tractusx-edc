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

package org.eclipse.tractusx.edc.lifecycle;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.iam.identitytrust.sts.embedded.EmbeddedSecureTokenService;
import org.eclipse.edc.identitytrust.SecureTokenService;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.tractusx.edc.token.MockBpnIdentityService;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

/**
 * This is an extension of the conventional {@link ParticipantRuntime}, that uses the new EDR V2 API, as well as DataPlane Signaling and
 * Token Refresh.
 * Once the old EDR API is gone, this should be merged with its superclass.
 */
public class SignalingParticipantRuntime extends ParticipantRuntime {
    public SignalingParticipantRuntime(String moduleName, String runtimeName, String bpn, Map<String, String> properties) {
        super(moduleName, runtimeName, bpn, properties);
        this.registerServiceMock(IdentityService.class, new MockBpnIdentityService(bpn));
        this.registerServiceMock(AudienceResolver.class, RemoteMessage::getCounterPartyAddress);
        var kid = properties.get("edc.iam.issuer.id") + "#key-1";
        try {
            var runtimeKeyPair = new ECKeyGenerator(Curve.P_256).keyID(kid).generate();
            var privateKey = runtimeKeyPair.toPrivateKey();

            registerServiceMock(SecureTokenService.class, new EmbeddedSecureTokenService(new JwtGenerationService(), () -> privateKey, () -> kid, Clock.systemUTC(), Duration.ofMinutes(10).toMillis()));
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }
}
