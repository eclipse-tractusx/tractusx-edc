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

import static org.eclipse.tractusx.edc.postgresql.migration.util.PolicyMigrationUtil.updateRules;

@SuppressWarnings("checkstyle:TypeName")
public class V0_0_7__Bpn_Namespace_Migration extends BaseJavaMigration {
    private final TypeReference<List<Permission>> permissionListType = new TypeReference<>() {
    };
    private final TypeReference<List<Prohibition>> prohibitionListType = new TypeReference<>() {
    };
    private final TypeReference<List<Duty>> dutyListType = new TypeReference<>() {
    };

    private final String updateStatement = "UPDATE edc_policydefinitions SET permissions = ?::json, prohibitions = ?::json, duties = ?::json WHERE policy_id = ?";
    private final String selectAllStatement = "SELECT * FROM edc_policydefinitions";

    @Override
    public void migrate(Context context) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        mapper.registerSubtypes(Permission.class, Prohibition.class, AtomicConstraint.class, AndConstraint.class, OrConstraint.class, XoneConstraint.class, LiteralExpression.class);

        try (var stmt = context.getConnection().createStatement(); var rs = stmt.executeQuery(selectAllStatement)) {
            while (rs.next()) {
                String id = rs.getString("policy_id");
                List<Rule> permissions = mapper.readValue(rs.getString("permissions"), new TypeReference<List<Rule>>() {
                });
                List<Rule> prohibitions = mapper.readValue(rs.getString("prohibitions"), new TypeReference<List<Rule>>() {
                });
                List<Rule> duties = mapper.readValue(rs.getString("duties"), new TypeReference<List<Rule>>() {
                });

                if (updateRules(permissions, prohibitions, duties)) {
                    updatePolicyInDb(context, id,
                            mapper.writerFor(permissionListType).writeValueAsString(permissions),
                            mapper.writerFor(prohibitionListType).writeValueAsString(prohibitions),
                            mapper.writerFor(dutyListType).writeValueAsString(duties));
                }
            }
        }
    }

    private void updatePolicyInDb(Context context, String id, String permissionsJson, String prohibitionsJson, String dutiesJson) {
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
