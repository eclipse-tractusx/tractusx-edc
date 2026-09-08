/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.edc.connector.controlplane.transform.odrl.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.participant.spi.ParticipantIdMapper;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_RIGHT_OPERAND_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A set-based operator carries a list of values, which the to-transformer keeps as unresolved JSON-LD literals.
 * Rendering the policy back must produce a JSON-LD array of those values, not the list's {@code toString()}.
 * A Catena-X {@code BusinessPartnerNumber} constraint is the practical case: it is defined as {@code isAnyOf}
 * over an array of BPNLs.
 */
class MultiValuedRightOperandTest {

    private static final String FIRST_BPN = "BPNL00000000AAAA";
    private static final String SECOND_BPN = "BPNL00000000BBBB";

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock();
    private final ParticipantIdMapper participantIdMapper = mock();

    private final JsonObjectFromPolicyTransformer transformer =
            new JsonObjectFromPolicyTransformer(jsonFactory, participantIdMapper);

    @BeforeEach
    void setUp() {
        when(participantIdMapper.toIri(any())).thenAnswer(it -> it.getArgument(0));
    }

    @Test
    void jsonLiterals_areRenderedAsAnArrayOfValues() {
        var rightValue = List.of(
                Json.createObjectBuilder().add(VALUE, FIRST_BPN).build(),
                Json.createObjectBuilder().add(VALUE, SECOND_BPN).build());

        assertThat(rightOperandValues(rightValue)).containsExactly(FIRST_BPN, SECOND_BPN);
    }

    @Test
    void deserializedLiterals_areRenderedAsAnArrayOfValues() {
        var rightValue = List.of(
                Map.of(VALUE, Map.of("chars", FIRST_BPN, "string", FIRST_BPN, "valueType", "STRING")),
                Map.of(VALUE, Map.of("chars", SECOND_BPN, "string", SECOND_BPN, "valueType", "STRING")));

        assertThat(rightOperandValues(rightValue)).containsExactly(FIRST_BPN, SECOND_BPN);
    }

    @Test
    void plainStrings_areRenderedAsAnArrayOfValues() {
        assertThat(rightOperandValues(List.of(FIRST_BPN, SECOND_BPN))).containsExactly(FIRST_BPN, SECOND_BPN);
    }

    @Test
    void singleValue_isStillRenderedAsOneValueObject() {
        var constraint = transformConstraint(FIRST_BPN);

        assertThat(constraint.getJsonObject(ODRL_RIGHT_OPERAND_ATTRIBUTE).getString(VALUE)).isEqualTo(FIRST_BPN);
    }

    private List<String> rightOperandValues(Object rightValue) {
        return transformConstraint(rightValue).getJsonArray(ODRL_RIGHT_OPERAND_ATTRIBUTE).stream()
                .map(JsonValue::asJsonObject)
                .map(entry -> entry.getString(VALUE))
                .toList();
    }

    private JsonObject transformConstraint(Object rightValue) {
        var constraint = AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression("BusinessPartnerNumber"))
                .operator(Operator.IS_ANY_OF)
                .rightExpression(new LiteralExpression(rightValue))
                .build();
        var policy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .action(Action.Builder.newInstance().type("access").build())
                        .constraint(constraint)
                        .build())
                .build();

        var result = transformer.transform(policy, context);

        assertThat(result).isNotNull();
        return result.getJsonArray(ODRL_PERMISSION_ATTRIBUTE).getJsonObject(0)
                .getJsonArray(ODRL_CONSTRAINT_ATTRIBUTE).getJsonObject(0);
    }
}
