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

package org.eclipse.tractusx.edc.tests.participant;

import com.nimbusds.jose.jwk.JWK;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participant;
import org.eclipse.edc.security.token.jwt.CryptoConverter;

import java.security.KeyPair;

import static org.eclipse.tractusx.edc.tests.helpers.Functions.generateKeyPair;

public abstract class IdentityParticipant extends Participant {
    private final KeyPair keyPair;
    private JWK keyPairJwk;

    public IdentityParticipant() {
        keyPair = generateKeyPair();
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public JWK getKeyPairAsJwk() {
        if (keyPairJwk == null) {
            var jwk = CryptoConverter.createJwk(keyPair).toJSONObject();
            jwk.put("kid", getFullKeyId());
            keyPairJwk = CryptoConverter.create(jwk);
        }
        return keyPairJwk;
    }

    public String getPrivateKeyAsString() {
        return CryptoConverter.createJwk(keyPair).toJSONString();
    }

    public String getPublicKeyAsString() {
        return CryptoConverter.createJwk(keyPair).toJSONString();
    }

    public String getPrivateKeyAlias() {
        return "private." + getFullKeyId();
    }

    public abstract String getFullKeyId();

    public String getKeyId() {
        return "key-1";
    }
}
