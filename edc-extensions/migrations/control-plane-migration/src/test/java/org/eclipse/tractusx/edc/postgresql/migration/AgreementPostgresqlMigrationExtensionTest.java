package org.eclipse.tractusx.edc.postgresql.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.SqlContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
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

import java.time.Clock;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.postgresql.migration.util.ContractNegotiationMigrationUtil.negotiation;
import static org.eclipse.tractusx.edc.postgresql.migration.util.PolicyMigrationUtil.andConstraint;
import static org.eclipse.tractusx.edc.postgresql.migration.util.PolicyMigrationUtil.atomicConstraint;
import static org.eclipse.tractusx.edc.postgresql.migration.util.PolicyMigrationUtil.constraintsWithLeftExpressions;
import static org.eclipse.tractusx.edc.postgresql.migration.util.PolicyMigrationUtil.permission;
import static org.eclipse.tractusx.edc.postgresql.migration.util.PolicyMigrationUtil.prohibition;

@ExtendWith(PostgresqlStoreSetupExtension.class)
public class AgreementPostgresqlMigrationExtensionTest {
    private final String oldBpgLeftOperand = "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerGroup";
    private final String updatedBpgLeftOperand = "https://w3id.org/catenax/2025/9/policy/BusinessPartnerGroup";
    private final String oldBpnLeftOperand = "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerNumber";
    private final String updatedBpnLeftOperand = "https://w3id.org/catenax/2025/9/policy/BusinessPartnerNumber";

    private SqlContractNegotiationStore store;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        TypeManager typeManager = new JacksonTypeManager();
        mapper = typeManager.getMapper();
        mapper.registerSubtypes(AtomicConstraint.class, LiteralExpression.class, AndConstraint.class, OrConstraint.class);

        store = new SqlContractNegotiationStore(
                extension.getDataSourceRegistry(),
                extension.getDatasourceName(),
                extension.getTransactionContext(),
                mapper,
                new PostgresDialectStatements(),
                "lease-holder-name",
                Clock.systemUTC(),
                queryExecutor
        );
    }

    @Test
    void version010shouldUpdateBusinessPartnerNamespace(PostgresqlStoreSetupExtension extension) {
        var dataSource = extension.getDataSourceRegistry().resolve(extension.getDatasourceName());

        FlywayManager.migrate(dataSource, "contractnegotiation", "public", MigrationVersion.fromVersion("0.0.9"));

        Policy policy = policyWithPermissionAndProhibition();
        insert(negotiation("1", policy));

        int oldBpgExpressions = constraintsWithLeftExpressions(policy, Set.of(oldBpgLeftOperand));
        int oldBpnExpressions = constraintsWithLeftExpressions(policy, Set.of(oldBpnLeftOperand));

        FlywayManager.migrate(dataSource, "contractnegotiation", "public", MigrationVersion.fromVersion("0.1.0"));
        var result = store.findById("1");

        assertThat(result).isNotNull();

        assertThat(constraintsWithLeftExpressions(result.getContractAgreement().getPolicy(), Set.of(oldBpgLeftOperand))).isEqualTo(0);
        assertThat(constraintsWithLeftExpressions(result.getContractAgreement().getPolicy(), Set.of(oldBpnLeftOperand))).isEqualTo(0);

        assertThat(constraintsWithLeftExpressions(result.getContractOffers().get(0).getPolicy(), Set.of(oldBpgLeftOperand))).isEqualTo(0);
        assertThat(constraintsWithLeftExpressions(result.getContractOffers().get(0).getPolicy(), Set.of(oldBpnLeftOperand))).isEqualTo(0);

        assertThat(constraintsWithLeftExpressions(result.getContractAgreement().getPolicy(), Set.of(updatedBpgLeftOperand))).isEqualTo(oldBpgExpressions);
        assertThat(constraintsWithLeftExpressions(result.getContractAgreement().getPolicy(), Set.of(updatedBpnLeftOperand))).isEqualTo(oldBpnExpressions);

        assertThat(constraintsWithLeftExpressions(result.getContractOffers().get(0).getPolicy(), Set.of(updatedBpgLeftOperand))).isEqualTo(oldBpgExpressions);
        assertThat(constraintsWithLeftExpressions(result.getContractOffers().get(0).getPolicy(), Set.of(updatedBpnLeftOperand))).isEqualTo(oldBpnExpressions);
    }

    private void insert(ContractNegotiation contractNegotiation) {
        store.save(contractNegotiation);
    }

    private Policy policyWithPermissionAndProhibition() {
        Permission permission = permission(andConstraint(atomicConstraint(oldBpgLeftOperand), atomicConstraint(oldBpnLeftOperand), atomicConstraint("otherExpression")));
        Prohibition prohibition = prohibition(atomicConstraint(oldBpnLeftOperand), atomicConstraint("otherExpression"));
        return Policy.Builder.newInstance()
                .permission(permission)
                .prohibition(prohibition)
                .target("test-asset-id")
                .build();
    }
}