/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.policy.cx.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.policy.cx.common.JsonLdTypeFunctions.extractObjectsOfType;
import static org.eclipse.tractusx.edc.policy.cx.common.JsonLdTypeFunctions.partitionByType;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyNamespaces.W3C_VC_PREFIX;
import static org.eclipse.tractusx.edc.policy.cx.fixtures.JsonLdTextFixtures.createObjectMapper;
import static org.eclipse.tractusx.edc.policy.cx.fixtures.JsonLdTextFixtures.expand;

class JsonLdTypeFunctionsTest {
    private static final String TYPE = "@type";
    private static final String VC_TYPE = W3C_VC_PREFIX + "#VerifiableCredential";

    private static final String BAR_CREDENTIAL_TYPE = "BarCredential";
    private static final String FOO_CREDENTIAL_TYPE = "FooCredential";

    @Test
    void verify_credential_extraction() throws JsonProcessingException {
        var vp = expand(createObjectMapper().readValue(SINGLE_VC_CLAIM, JsonObject.class), Map.of());

        var credentials = extractObjectsOfType(VC_TYPE, vp).toList();

        assertThat(credentials.size()).isEqualTo(1);
        assertAllOfType(FOO_CREDENTIAL_TYPE, credentials);
    }

    @Test
    void verify_partitions_based_on_type() throws JsonProcessingException {
        var vp = expand(createObjectMapper().readValue(MULTIPLE_VCS_CLAIM, JsonObject.class), Map.of());

        var credentials = extractObjectsOfType(VC_TYPE, vp);
        var partitions = partitionByType(credentials);

        assertThat(partitions.size()).isEqualTo(3);

        assertAllOfType(FOO_CREDENTIAL_TYPE, partitions.get(FOO_CREDENTIAL_TYPE));
        assertAllOfType(BAR_CREDENTIAL_TYPE, partitions.get(BAR_CREDENTIAL_TYPE));
        assertThat(partitions.get(VC_TYPE).size()).isEqualTo(2);
    }

    /**
     * Asserts that all objects on the collection are of a given type.
     */
    private void assertAllOfType(String type, List<JsonObject> objects) {
        assertThat(objects.stream()
                .flatMap(object -> object.get(TYPE).asJsonArray().stream())
                .filter(value -> value instanceof JsonString)
                .filter(entryType -> type.equals(((JsonString) entryType).getString()))
                .count()).isEqualTo(objects.size());
    }

    private static final String FOO_CREDENTIAL = """
            {
              "type": "VerifiablePresentation",
              "verifiableCredential": [
                {
                  "@context": [
                    "https://www.w3.org/2018/credentials/v1"
                  ],
                  "id": "urn:uuid:12345678-1234-1234-1234-123456789abc",
                  "type": [
                    "VerifiableCredential",
                    "FooCredential"
                  ]
                }
              ]
            }""";

    private static final String BAR_CREDENTIAL = """
            {
              "type": "VerifiablePresentation",
              "verifiableCredential": [
                {
                  "@context": [
                    "https://www.w3.org/2018/credentials/v1"
                  ],
                  "type": [
                    "VerifiableCredential",
                    "BarCredential"
                  ]
                }
              ]
            }""";

    private static final String MULTIPLE_VCS_CLAIM = format("""
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1",
                {
                  "vp":"test:vp"
                }
              ],
              "vp": [%s,%s]
            }""", FOO_CREDENTIAL, BAR_CREDENTIAL);

    private static final String SINGLE_VC_CLAIM = format("""
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1",
                {
                  "vp":"test:vp"
                }
              ],
              "vp": %s
            }""", FOO_CREDENTIAL);


}
