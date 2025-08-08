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

package org.eclipse.tractusx.edc.postgresql.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.store.sql.policydefinition.store.SqlPolicyDefinitionStore;
import org.eclipse.edc.connector.controlplane.store.sql.policydefinition.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.postgresql.migration.util.PolicyMigrationUtil.numberOfConstraintsInRulesWithLeftExpression;

//@PostgresqlIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
public class PolicyPostgresqlMigrationExtensionTest {
    private final String oldBpgLeftOperand = "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerGroup";
    private final String updatedBpgLeftOperand = "https://w3id.org/catenax/2025/9/policy/BusinessPartnerGroup";
    private final String oldBpnLeftOperand = "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerNumber";
    private final String updatedBpnLeftOperand = "https://w3id.org/catenax/2025/9/policy/BusinessPartnerNumber";

    private SqlPolicyDefinitionStore store;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        TypeManager typeManager = new JacksonTypeManager();
        mapper = typeManager.getMapper();
        mapper.registerSubtypes(AtomicConstraint.class, LiteralExpression.class, AndConstraint.class, OrConstraint.class);

        store = new SqlPolicyDefinitionStore(
                extension.getDataSourceRegistry(),
                extension.getDatasourceName(),
                extension.getTransactionContext(),
                mapper,
                new PostgresDialectStatements(),
                queryExecutor
        );
    }

    private Policy policyWithPermissionAndProhibition() {
        Permission permission = permission(andConstraint(atomicConstraint(oldBpgLeftOperand), atomicConstraint(oldBpnLeftOperand), atomicConstraint("otherExpression")));
        Prohibition prohibition = prohibition(atomicConstraint(oldBpnLeftOperand), atomicConstraint("otherExpression"));
        return Policy.Builder.newInstance()
                .permission(permission)
                .prohibition(prohibition)
                .build();
    }

    private PolicyDefinition policyDefinition(String id, Policy policy) {
        return PolicyDefinition.Builder.newInstance()
                .id(id)
                .policy(policy)
                .createdAt(System.currentTimeMillis())
                .build();
    }

    @Test
    void version007Test(PostgresqlStoreSetupExtension extension) {
        var dataSource = extension.getDataSourceRegistry().resolve(extension.getDatasourceName());

        FlywayManager.migrate(dataSource, "policy", "public", MigrationVersion.fromVersion("0.0.5"));

        var policy = policyWithPermissionAndProhibition();
        insert(policyDefinition("1", policy));

        FlywayManager.migrate(dataSource, "policy", "public", MigrationVersion.fromVersion("0.0.7"));

        int oldBpgExpressions = constraintsWithLeftExpressions(policy, Set.of(oldBpgLeftOperand));
        int oldBpnExpressions = constraintsWithLeftExpressions(policy, Set.of(oldBpnLeftOperand));

        var result = store.findById("1");

        assertThat(result).isNotNull();

        int updatedBpgExpressions = constraintsWithLeftExpressions(result.getPolicy(), Set.of(updatedBpgLeftOperand));
        int updatedBpnExpressions = constraintsWithLeftExpressions(result.getPolicy(), Set.of(updatedBpnLeftOperand));

        assertThat(constraintsWithLeftExpressions(result.getPolicy(), Set.of(oldBpgLeftOperand))).isEqualTo(0);
        assertThat(constraintsWithLeftExpressions(result.getPolicy(), Set.of(oldBpnLeftOperand))).isEqualTo(0);

        assertThat(updatedBpgExpressions).isEqualTo(oldBpgExpressions);
        assertThat(updatedBpnExpressions).isEqualTo(oldBpnExpressions);
    }

    private int constraintsWithLeftExpressions(Policy policy, Set<String> leftExpressions) {
        return numberOfConstraintsInRulesWithLeftExpression(policy.getPermissions(), leftExpressions) +
                numberOfConstraintsInRulesWithLeftExpression(policy.getProhibitions(), leftExpressions) +
                numberOfConstraintsInRulesWithLeftExpression(policy.getObligations(), leftExpressions);
    }

    private void insert(PolicyDefinition policyDefinition) {
        store.create(policyDefinition);
    }

    private Constraint andConstraint(Constraint... constraints) {
        return AndConstraint.Builder.newInstance()
                .constraints(Arrays.asList(constraints))
                .build();
    }

    private Constraint atomicConstraint(String leftExpression) {
        return AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression(leftExpression))
                .rightExpression(new LiteralExpression("right-value"))
                .operator(Operator.EQ)
                .build();
    }

    private Permission permission(Constraint... constraints) {
        return Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("use").build())
                .constraints(Arrays.asList(constraints))
                .build();
    }

    private Prohibition prohibition(Constraint... constraints) {
        return Prohibition.Builder.newInstance()
                .action(Action.Builder.newInstance().type("use").build())
                .constraints(Arrays.asList(constraints))
                .build();
    }

}