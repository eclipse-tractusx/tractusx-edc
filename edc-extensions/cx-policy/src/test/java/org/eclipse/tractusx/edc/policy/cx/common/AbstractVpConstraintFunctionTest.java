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

package org.eclipse.tractusx.edc.policy.cx.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdTextFixtures.createObjectMapper;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdTextFixtures.expand;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AbstractVpConstraintFunctionTest {
    private static final String FOO_CREDENTIAL = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1"
              ],
              "id": "urn:uuid:12345678-1234-1234-1234-123456789abc",
              "type": [
                "VerifiableCredential",
                "FooCredential"
              ],
              "issuer": "did:web:test",
              "credentialSubject": {
                "id": "did:web:test"
              }
            }
            """;
    private static final String PRESENTATION = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1"
              ],
              "type": "VerifiablePresentation"
            }
            """;
    private AbstractVpConstraintFunction function;
    private PolicyContext context;

    @Test
    void verify_operators() {
        assertThat(function.validateOperator(Operator.EQ, context, Operator.EQ)).isEqualTo(true);
    }

    @Test
    void verify_invalid_operators() {
        assertThat(function.validateOperator(Operator.NEQ, context, Operator.EQ)).isEqualTo(false);
        verify(context).reportProblem(anyString());
    }

    @Test
    void verify_presentation() throws JsonProcessingException {
        var vp = expand(createObjectMapper().readValue(PRESENTATION, JsonObject.class), Map.of());

        assertThat(function.validatePresentation(vp, context)).isTrue();

        assertThat(function.validatePresentation(null, context)).isFalse();

        assertThat(function.validatePresentation("invalid", context)).isFalse();

        var array = Json.createArrayBuilder().build();
        assertThat(function.validatePresentation(array, context)).isFalse();
    }

    @Test
    void verify_extract_credential_subject() throws JsonProcessingException {
        var credential = expand(createObjectMapper().readValue(FOO_CREDENTIAL, JsonObject.class), Map.of());

        var subject = function.extractCredentialSubject(credential, context);

        assertThat(subject).isNotNull();
        assertThat(((JsonString) subject.get("@id")).getString()).isEqualTo("did:web:test");
    }

    @BeforeEach
    void setUp() {
        context = mock(PolicyContext.class);
        function = new AbstractVpConstraintFunction("FooCredential") {
            @Override
            public boolean evaluate(Operator operator, Object rightValue, Permission rule, PolicyContext context) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
