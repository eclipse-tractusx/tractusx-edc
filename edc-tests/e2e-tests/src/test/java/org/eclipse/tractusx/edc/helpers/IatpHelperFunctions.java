/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.helpers;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.eclipse.edc.spi.EdcException;
import org.testcontainers.shaded.org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.io.StringWriter;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.function.Supplier;

public class IatpHelperFunctions {

    /**
     * Returns the Pem representation of a {@link Key}
     *
     * @param key The input key
     * @return The pem encoded key
     */
    public static String toPemEncoded(Key key) {
        var writer = new StringWriter();
        try (var jcaPEMWriter = new JcaPEMWriter(writer)) {
            jcaPEMWriter.writeObject(key);
        } catch (IOException e) {
            throw new EdcException("Unable to convert private in PEM format ", e);
        }
        return writer.toString();
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

    public static JsonObject createVc(String issuer, String type, Supplier<JsonObject> subjectSupplier) {
        return Json.createObjectBuilder()
                .add("@context", context())
                .add("type", types(type))
                .add("credentialSubject", subjectSupplier.get())
                .add("issuer", issuer)
                .add("issuanceDate", Instant.now().toString())
                .build();
    }

    public static JsonObject membershipSubject(String did, String id) {
        return Json.createObjectBuilder()
                .add("type", "MembershipCredential")
                .add("holderIdentifier", id)
                .add("status", "Active")
                .add("memberOf", "Catena-X")
                .add("startTime", Instant.now().toString())
                .add("id", did)
                .build();

    }

    public static JsonObject frameworkAgreementSubject(String did, String id, String type) {
        return Json.createObjectBuilder()
                .add("type", type)
                .add("holderIdentifier", id)
                .add("useCaseType", type)
                .add("contractVersion", "1.0.0")
                .add("contractTemplate", "https://public.catena-x.org/contracts/traceabilty.v1.pdf")
                .add("id", did)
                .build();

    }

    private static JsonArray types(String type) {
        return Json.createArrayBuilder()
                .add("VerifiableCredential")
                .add(type)
                .build();
    }

    private static JsonArray context() {
        return Json.createArrayBuilder()
                .add("https://www.w3.org/2018/credentials/v1")
                .add("https://w3id.org/security/suites/jws-2020/v1")
                .add("https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json")
                .build();
    }
}
