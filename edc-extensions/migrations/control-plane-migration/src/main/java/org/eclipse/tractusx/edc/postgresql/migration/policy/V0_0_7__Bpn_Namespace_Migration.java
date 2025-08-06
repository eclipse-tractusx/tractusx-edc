/********************************************************************************
 * Copyright (c) 2025 Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
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

package org.eclipse.tractusx.edc.postgresql.migration.policy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.policy.model.XoneConstraint;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@SuppressWarnings("checkstyle:TypeName")
public class V0_0_7__Bpn_Namespace_Migration extends BaseJavaMigration {
    private final TypeReference<List<Permission>> permissionListType = new TypeReference<>() {
    };
    private final TypeReference<List<Prohibition>> prohibitionListType = new TypeReference<>() {
    };
    private final TypeReference<List<Duty>> dutyListType = new TypeReference<>() {
    };

    private final String oldBpgLeftOperand = "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerGroup";
    private final String updatedBpgLeftOperand = "https://w3id.org/catenax/2025/9/policy/BusinessPartnerGroup";
    private final String oldBpnLeftOperand = "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerNumber";
    private final String updatedBpnLeftOperand = "https://w3id.org/catenax/2025/9/policy/BusinessPartnerNumber";

    private final String updateStatement = "UPDATE edc_policydefinitions SET permissions = ?::json, prohibitions = ?::json, duties = ?::json WHERE policy_id = ?";

    @Override
    public void migrate(Context context) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(Permission.class);
        mapper.registerSubtypes(Prohibition.class);
        mapper.registerSubtypes(AtomicConstraint.class, AndConstraint.class, OrConstraint.class, XoneConstraint.class, LiteralExpression.class);

        try (var stmt = context.getConnection().createStatement();
             var rs = stmt.executeQuery("SELECT * FROM edc_policydefinitions")) {
            while (rs.next()) {
                String id = rs.getString("policy_id");
                List<Rule> permissions = ruleDeserializer(mapper, rs.getString("permissions"));
                List<Rule> prohibitions = ruleDeserializer(mapper, rs.getString("prohibitions"));
                List<Rule> duties = ruleDeserializer(mapper, rs.getString("duties"));

                if (containsBusinessPartnerRules(permissions) || containsBusinessPartnerRules(prohibitions) || containsBusinessPartnerRules(duties)) {
                    updatePolicy(context, id,
                            ruleSerializer(mapper, permissions, permissionListType),
                            ruleSerializer(mapper, prohibitions, prohibitionListType),
                            ruleSerializer(mapper, duties, dutyListType));
                }
                System.out.println(permissions);
            }
        }
    }

    private void updatePolicy(Context context, String id, String permissionsJson, String prohibitionsJson, String dutiesJson) {
        try (PreparedStatement ps = context.getConnection().prepareStatement(updateStatement)) {
            ps.setString(1, permissionsJson);
            ps.setString(2, prohibitionsJson);
            ps.setString(3, dutiesJson);
            ps.setString(4, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private List<Rule> ruleDeserializer(ObjectMapper mapper, String ruleJson) throws JsonProcessingException {
        return mapper.readValue(ruleJson, new TypeReference<List<Rule>>() {
        });
    }

    private <T> String ruleSerializer(ObjectMapper mapper, List<Rule> rules, TypeReference<T> typeReference) throws JsonProcessingException {
        return mapper.writerFor(typeReference).writeValueAsString(rules);
    }

    private boolean containsBusinessPartnerRules(List<Rule> rules) {
        boolean containsBpRules = false;
        for (Rule rule : rules) {
            List<Constraint> constraints = rule.getConstraints();
            if (containsBusinessPartnerConstraints(constraints)) {
                containsBpRules = true;
            }
        }
        return containsBpRules;
    }

    private boolean containsBusinessPartnerConstraints(List<Constraint> constraints) {
        boolean containsBpConstraint = false;
        for (int i = 0; i < constraints.size(); i++) {
            var constraint = constraints.get(i);
            if (constraint instanceof AtomicConstraint) {
                if (containsBusinessPartnerOperand((AtomicConstraint) constraint)) {
                    containsBpConstraint = true;
                    Constraint updatedConstraint = updateBpnLeftOperand(constraint);
                    constraints.set(i, updatedConstraint);
                }
            } else if (constraint instanceof MultiplicityConstraint) {
                if (containsBusinessPartnerConstraints(((MultiplicityConstraint) constraint).getConstraints())) {
                    containsBpConstraint = true;
                }
            }
        }
        return containsBpConstraint;
    }

    private boolean containsBusinessPartnerOperand(AtomicConstraint atomicConstraint) {
        var leftExpressionValue = getLeftExpressionValue(atomicConstraint);
        return leftExpressionValue != null && (leftExpressionValue.equals(oldBpgLeftOperand) || leftExpressionValue.equals(oldBpnLeftOperand));
    }

    private Object getLeftExpressionValue(AtomicConstraint atomicConstraint) {
        if (atomicConstraint.getLeftExpression() instanceof LiteralExpression literalExpression) {
            return literalExpression.getValue();
        }
        return null;
    }

    private Constraint updateBpnLeftOperand(Constraint constraint) {
        AtomicConstraint atomicConstraint = (AtomicConstraint) constraint;
        var leftExpressionValue = getLeftExpressionValue(atomicConstraint);

        String updatedLiteral = leftExpressionValue != null && leftExpressionValue.equals(oldBpgLeftOperand) ? updatedBpgLeftOperand : updatedBpnLeftOperand;

        AtomicConstraint updatedAtomicConstraint = AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression(updatedLiteral))
                .operator(atomicConstraint.getOperator())
                .rightExpression(atomicConstraint.getRightExpression())
                .build();
        return updatedAtomicConstraint;
    }
}
