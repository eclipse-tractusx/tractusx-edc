package org.eclipse.tractusx.edc.postgresql.migration.contractnegotiation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
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

import static org.eclipse.tractusx.edc.postgresql.migration.util.PolicyMigrationUtil.policyDeserializer;
import static org.eclipse.tractusx.edc.postgresql.migration.util.PolicyMigrationUtil.policySerializer;
import static org.eclipse.tractusx.edc.postgresql.migration.util.PolicyMigrationUtil.rulesContainsLeftExpression;
import static org.eclipse.tractusx.edc.postgresql.migration.util.PolicyMigrationUtil.updateBusinessPartnerRules;

public class V0_1_0__Bpn_Namespace_Migration_For_Agreement extends BaseJavaMigration {
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

    private final String updateStatement = "UPDATE edc_contract_agreement SET policy = ?::json WHERE agr_id = ?";
    private final String selectAllStatement = "SELECT * FROM edc_contract_agreement";

    @Override
    public void migrate(Context context) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(Permission.class);
        mapper.registerSubtypes(Prohibition.class);
        mapper.registerSubtypes(AtomicConstraint.class, AndConstraint.class, OrConstraint.class, XoneConstraint.class, LiteralExpression.class);

        try (var stmt = context.getConnection().createStatement();
             var rs = stmt.executeQuery(selectAllStatement)) {
            while (rs.next()) {
                String id = rs.getString("agr_id");
                String policyId = rs.getString("policy_id");
                String policyJson = rs.getString("policy");
                Policy policy = policyDeserializer(mapper, policyJson);
                List<Permission> permissions = policy.getPermissions();
                List<Prohibition> prohibitions = policy.getProhibitions();
                List<Duty> duties = policy.getObligations();

                updateRules(context, mapper, id, policy, permissions, prohibitions, duties);
            }
        }
    }

    private void updateRules(Context context, ObjectMapper mapper, String id, Policy policy, List<? extends Rule> permissions, List<? extends Rule> prohibitions, List<? extends Rule> duties) throws JsonProcessingException {
        boolean permissionsUpdated = updateRules(policy.getPermissions());
        boolean prohibitionsUpdated = updateRules(policy.getProhibitions());
        boolean dutiesUpdated = updateRules(policy.getObligations());

        if (permissionsUpdated || prohibitionsUpdated || dutiesUpdated) {
            updatePolicyInDB(context, id, policySerializer(mapper, policy));
        }
    }

    private boolean updateRules(List<? extends Rule> rules) {
        if (rulesContainsLeftExpression(rules, oldBpnLeftExpressions)) {
            updateBusinessPartnerRules(rules, updateLeftExpressions);
            return true;
        }
        return false;
    }

    private void updatePolicyInDB(Context context, String id, String policy) {
        try (PreparedStatement ps = context.getConnection().prepareStatement(updateStatement)) {
            ps.setString(1, policy);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database update failed", e);
        }
    }
}
