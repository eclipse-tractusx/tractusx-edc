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

package org.eclipse.tractusx.edc.tests.transfer.iatp.harness;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.BitString;
import org.eclipse.edc.spi.EdcException;

import java.util.List;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;

public class StatusList2021 {
    private final BitString bitString = BitString.Builder.newInstance().size(16 * 1024 * 8).build(); //minimum size is 16KB
    private final String issuer;
    private final String purpose;

    public StatusList2021(String issuer, String purpose) {
        this.issuer = issuer;
        this.purpose = purpose;
    }

    public static StatusList2021 create(String issuer, String purpose) {
        return new StatusList2021(issuer, purpose);
    }

    public StatusList2021 withStatus(int index, boolean status) {
        bitString.set(index, status);
        return this;
    }

    public JsonObject toJsonObject() {
        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createArrayBuilder()
                        .add("https://www.w3.org/2018/credentials/v1")
                        .add("https://w3id.org/vc/status-list/2021/v1"))
                .add("id", "https://example.com/credentials/23894672394")
                .add("type", Json.createArrayBuilder(List.of("VerifiableCredential", "StatusList2021Credential")))
                .add("issuer", issuer)
                .add("issued", "2021-04-05T14:27:40Z")
                .add("credentialSubject", Json.createArrayBuilder().add(
                        Json.createObjectBuilder()
                                .add("id", "https://example.com/status/3#list")
                                .add("type", "StatusList2021")
                                .add("statusPurpose", purpose)
                                .add("encodedList", createEncodedList())))
                .build();

    }

    private String createEncodedList() {
        return BitString.Writer.newInstance()
                .write(bitString)
                .orElseThrow((f) -> new EdcException(f.getFailureDetail()));
    }
}
