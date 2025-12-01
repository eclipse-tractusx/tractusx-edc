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

package org.eclipse.tractusx.edc.tests.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.Request;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECGenParameterSpec;

public class Functions {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ReceivedEvent readEvent(Request request) {
        try {
            return MAPPER.readValue(request.getBody(), ReceivedEvent.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider());
            gen.initialize(new ECGenParameterSpec("secp256r1"));
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }
}
