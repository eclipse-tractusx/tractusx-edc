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
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.LiteralExpression;
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
import java.util.Map;
import java.util.Set;

import static org.eclipse.tractusx.edc.postgresql.migration.util.PolicyMigrationUtil.ruleDeserializer;
import static org.eclipse.tractusx.edc.postgresql.migration.util.PolicyMigrationUtil.ruleSerializer;
import static org.eclipse.tractusx.edc.postgresql.migration.util.PolicyMigrationUtil.rulesContainsLeftExpression;
import static org.eclipse.tractusx.edc.postgresql.migration.util.PolicyMigrationUtil.updateBusinessPartnerRules;

@SuppressWarnings("checkstyle:TypeName")
public class V0_0_7__Bpn_Namespace_Migration extends BaseJavaMigration {
    private final TypeReference<List<Permission>> permissionListType = new TypeReference<>() {
    };
    private final TypeReference<List<Prohibition>> prohibitionListType = new TypeReference<>() {
    };
    private final TypeReference<List<Duty>> dutyListType = new TypeReference<>() {
    };

    private final Set<String> oldBpnLeftExpressions = Set.of(
            "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerGroup",
            "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerNumber"
    );

    private final Map<String, String> updateLeftExpressions = Map.of(
            "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerGroup", "https://w3id.org/catenax/2025/9/policy/BusinessPartnerGroup",
            "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerNumber", "https://w3id.org/catenax/2025/9/policy/BusinessPartnerNumber"
    );

    private final String updateStatement = "UPDATE edc_policydefinitions SET permissions = ?::json, prohibitions = ?::json, duties = ?::json WHERE policy_id = ?";
    private final String selectAllStatement = "SELECT * FROM edc_policydefinitions";

    @Override
    public void migrate(Context context) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(Permission.class);
        mapper.registerSubtypes(Prohibition.class);
        mapper.registerSubtypes(AtomicConstraint.class, AndConstraint.class, OrConstraint.class, XoneConstraint.class, LiteralExpression.class);

        try (var stmt = context.getConnection().createStatement();
             var rs = stmt.executeQuery(selectAllStatement)) {
            while (rs.next()) {
                String id = rs.getString("policy_id");
                List<Rule> permissions = ruleDeserializer(mapper, rs.getString("permissions"));
                List<Rule> prohibitions = ruleDeserializer(mapper, rs.getString("prohibitions"));
                List<Rule> duties = ruleDeserializer(mapper, rs.getString("duties"));

                updateRules(context, mapper, id, permissions, prohibitions, duties);
            }
        }
    }

    private void updateRules(Context context, ObjectMapper mapper, String id, List<Rule> permissions, List<Rule> prohibitions, List<Rule> duties) throws JsonProcessingException {
        boolean permissionsUpdated = updateRules(permissions);
        boolean prohibitionsUpdated = updateRules(prohibitions);
        boolean dutiesUpdated = updateRules(duties);

        if (permissionsUpdated || prohibitionsUpdated || dutiesUpdated) {
            updatePolicyInDB(context, id,
                    ruleSerializer(mapper, permissions, permissionListType),
                    ruleSerializer(mapper, prohibitions, prohibitionListType),
                    ruleSerializer(mapper, duties, dutyListType));
        }
    }

    private boolean updateRules(List<Rule> rules) {
        if (rulesContainsLeftExpression(rules, oldBpnLeftExpressions)) {
            updateBusinessPartnerRules(rules, updateLeftExpressions);
            return true;
        }
        return false;
    }

    private void updatePolicyInDB(Context context, String id, String permissionsJson, String prohibitionsJson, String dutiesJson) {
        try (PreparedStatement ps = context.getConnection().prepareStatement(updateStatement)) {
            ps.setString(1, permissionsJson);
            ps.setString(2, prohibitionsJson);
            ps.setString(3, dutiesJson);
            ps.setString(4, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database update failed", e);
        }
    }
}
