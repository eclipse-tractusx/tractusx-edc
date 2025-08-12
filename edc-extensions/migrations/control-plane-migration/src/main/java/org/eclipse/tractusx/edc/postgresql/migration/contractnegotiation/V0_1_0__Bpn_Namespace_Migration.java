package org.eclipse.tractusx.edc.postgresql.migration.contractnegotiation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.XoneConstraint;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.eclipse.tractusx.edc.postgresql.migration.util.PolicyMigrationUtil.updatePolicy;

public class V0_1_0__Bpn_Namespace_Migration extends BaseJavaMigration {

    private final String updateAgreementStatement = "UPDATE edc_contract_agreement SET policy = ?::json WHERE agr_id = ?";
    private final String updateNegotiationStatement = "UPDATE edc_contract_negotiation SET contract_offers = ?::json WHERE id = ?";
    private final String selectAllAgreementStatement = "SELECT * FROM edc_contract_agreement";
    private final String selectAllNegotiationStatement = "SELECT * FROM edc_contract_negotiation";

    @Override
    public void migrate(Context context) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        mapper.registerSubtypes(Permission.class, Prohibition.class, AtomicConstraint.class, AndConstraint.class, OrConstraint.class, XoneConstraint.class, LiteralExpression.class);

        try (var stmt = context.getConnection().createStatement();
             var rs = stmt.executeQuery(selectAllAgreementStatement)) {
            while (rs.next()) {
                String id = rs.getString("agr_id");
                String policyJson = rs.getString("policy");
                Policy policy = mapper.readValue(policyJson, new TypeReference<Policy>() {
                });

                if (updatePolicy(policy)) {
                    updateAgreementInDB(context, id, mapper.writeValueAsString(policy));
                }
            }
        }

        try (var stmt = context.getConnection().createStatement();
             var rs = stmt.executeQuery(selectAllNegotiationStatement)) {
            while (rs.next()) {
                String id = rs.getString("id");
                String contractOffersJson = rs.getString("contract_offers");
                List<ContractOffer> contractOffers = mapper.readValue(contractOffersJson, new TypeReference<List<ContractOffer>>() {
                });

                if (updateContractOffers(context, mapper, id, contractOffers)) {
                    updateNegotiationInDB(context, id, mapper.writeValueAsString(contractOffers));
                }
            }
        }
    }

    private boolean updateContractOffers(Context context, ObjectMapper mapper, String id, List<ContractOffer> contractOffers) throws JsonProcessingException {
        boolean isOfferUpdated = false;
        for (ContractOffer offer : contractOffers) {
            if (updatePolicy(offer.getPolicy())) {
                isOfferUpdated = true;
            }
        }
        return isOfferUpdated;
    }


    private void updateAgreementInDB(Context context, String id, String policy) {
        try (PreparedStatement ps = context.getConnection().prepareStatement(updateAgreementStatement)) {
            ps.setString(1, policy);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database update failed", e);
        }
    }

    private void updateNegotiationInDB(Context context, String id, String contractOffers) {
        try (PreparedStatement ps = context.getConnection().prepareStatement(updateNegotiationStatement)) {
            ps.setString(1, contractOffers);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database update failed", e);
        }
    }
}
