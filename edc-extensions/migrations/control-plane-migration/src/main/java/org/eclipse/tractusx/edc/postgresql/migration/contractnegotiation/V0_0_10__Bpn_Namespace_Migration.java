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

@SuppressWarnings("checkstyle:TypeName")
public class V0_0_10__Bpn_Namespace_Migration extends BaseJavaMigration {

    private final String updateAgreementStatement = "UPDATE edc_contract_agreement SET policy = ?::json WHERE agr_id = ?";
    private final String updateNegotiationStatement = "UPDATE edc_contract_negotiation SET contract_offers = ?::json WHERE id = ?";
    private final String selectAllAgreementStatement = "SELECT * FROM edc_contract_agreement";
    private final String selectAllNegotiationStatement = "SELECT * FROM edc_contract_negotiation";

    @Override
    public void migrate(Context context) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        mapper.registerSubtypes(Permission.class, Prohibition.class, AtomicConstraint.class, AndConstraint.class, OrConstraint.class, XoneConstraint.class, LiteralExpression.class);

        var connection = context.getConnection();
        try (var selectAgreements = connection.createStatement();
             var selectNegotiations = connection.createStatement();
             var updateAgreement = connection.prepareStatement(updateAgreementStatement);
             var updateNegotiation = connection.prepareStatement(updateNegotiationStatement)) {

            try (var rs = selectAgreements.executeQuery(selectAllAgreementStatement)) {
                while (rs.next()) {
                    String id = rs.getString("agr_id");
                    String policyJson = rs.getString("policy");
                    Policy policy = mapper.readValue(policyJson, new TypeReference<Policy>() {
                    });

                    if (updatePolicy(policy)) {
                        updateAgreementInDb(updateAgreement, id, mapper.writeValueAsString(policy));
                    }
                }
            }
            try (var rs = selectNegotiations.executeQuery(selectAllNegotiationStatement)) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String contractOffersJson = rs.getString("contract_offers");
                    List<ContractOffer> contractOffers = mapper.readValue(contractOffersJson, new TypeReference<List<ContractOffer>>() {
                    });

                    if (updateContractOffers(contractOffers)) {
                        updateNegotiationInDb(updateNegotiation, id, mapper.writeValueAsString(contractOffers));
                    }
                }
            }
        }
    }

    private boolean updateContractOffers(List<ContractOffer> contractOffers) throws JsonProcessingException {
        boolean isOfferUpdated = false;
        for (ContractOffer offer : contractOffers) {
            if (updatePolicy(offer.getPolicy())) {
                isOfferUpdated = true;
            }
        }
        return isOfferUpdated;
    }


    private void updateAgreementInDb(PreparedStatement ps, String id, String policy) {
        try {
            ps.setString(1, policy);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database update failed", e);
        }
    }

    private void updateNegotiationInDb(PreparedStatement ps, String id, String contractOffers) {
        try {
            ps.setString(1, contractOffers);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database update failed", e);
        }
    }
}
