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

package org.eclipse.tractusx.edc.policy.cx.summary;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces;
import org.eclipse.tractusx.edc.policy.cx.common.AbstractVpConstraintFunction;

import java.util.Map;

import static jakarta.json.JsonValue.ValueType.ARRAY;
import static jakarta.json.JsonValue.ValueType.OBJECT;
import static jakarta.json.JsonValue.ValueType.STRING;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.SUMMARY_CREDENTIAL_TYPE;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.VP_PROPERTY;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdTypeFunctions.extractObjectsOfType;


/**
 * Implements Catena-X policies by verifying policy constraints against the summary credential.
 * <p>
 * Verifies the presence of an entry in the {@link #SUMMARY_CREDENTIAL_ITEMS} of a summary credential token.
 */
public class SummaryConstraintFunction extends AbstractVpConstraintFunction {
    private static final String SUMMARY_CREDENTIAL_ITEMS = CredentialsNamespaces.CX_SUMMARY_NS + "/items";
    private static final String CREDENTIAL_SUBJECT = "credentialSubject";

    private static final String ACTIVE = "active";

    private final String summaryType;

    public SummaryConstraintFunction(String summaryType) {
        super("Summary");
        requireNonNull(summaryType);
        this.summaryType = summaryType;
    }

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, PolicyContext context) {
        if (!validateOperator(operator, context, EQ)) {
            return false;
        }

        if (!validateRightOperand(ACTIVE, rightValue, context)) {
            return false;
        }

        var vp = (JsonObject) context.getContextData(ParticipantAgent.class).getClaims().get(VP_PROPERTY);
        if (!validatePresentation(vp, context)) {
            return false;
        }

        return extractObjectsOfType(SUMMARY_CREDENTIAL_TYPE, vp).anyMatch(credential -> hasSummaryType(credential, context));
    }

    /**
     * Returns true if the summary credential has the item specified by {@link #summaryType}.
     */
    private boolean hasSummaryType(JsonObject credential, PolicyContext context) {
        var credentialSubject = extractCredentialSubject(credential, context);
        if (credentialSubject == null) {
            return false;
        }
        var items = credentialSubject.get(SUMMARY_CREDENTIAL_ITEMS);

        if (items == null || items.getValueType() != ARRAY) {
            context.reportProblem(format("%s items not found in %s", errorPrefix, CREDENTIAL_SUBJECT));
            return false;
        }

        if (items.asJsonArray().isEmpty()) {
            context.reportProblem(format("%s empty %s items graph container", errorPrefix, CREDENTIAL_SUBJECT));
            return false;
        }

        return items.asJsonArray().stream().filter(e -> e.getValueType() == OBJECT)
                .flatMap(o -> o.asJsonObject().entrySet().stream())
                .anyMatch(this::matchSummaryType);
    }

    /**
     * Returns true if the entry is a string and matches the Json-Ld {@link #VALUE} type.
     */
    private boolean matchSummaryType(Map.Entry<String, JsonValue> e) {
        return VALUE.equals(e.getKey()) &&
                e.getValue().getValueType() == STRING &&
                summaryType.equals(((JsonString) e.getValue()).getString());
    }


}
