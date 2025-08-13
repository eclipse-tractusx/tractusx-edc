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

package org.eclipse.tractusx.edc.policy.cx.validator;

import java.util.Set;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_AND_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_AND_SEQUENCE_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OR_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_XONE_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;

public final class PolicyValidationConstants {
    private PolicyValidationConstants() {
    }

    public static final String ACTION_ACCESS = EDC_NAMESPACE + "access";
    public static final String ACTION_USAGE = ODRL_SCHEMA + "use";

    public static final String FRAMEWORK_AGREEMENT_LITERAL = CX_POLICY_NS + "FrameworkAgreement";
    public static final String MEMBERSHIP_LITERAL = CX_POLICY_NS + "Membership";
    public static final String BUSINESS_PARTNER_GROUP_LITERAL = TX_NAMESPACE + "BusinessPartnerGroup";
    public static final String BUSINESS_PARTNER_NUMBER_LITERAL = TX_NAMESPACE + "BusinessPartnerNumber";
    public static final String USAGE_PURPOSE_LITERAL = CX_POLICY_NS + "UsagePurpose";
    public static final String AFFILIATES_REGION_LITERAL = CX_POLICY_NS + "AffiliatesRegion";
    public static final String AFFILIATES_BPNL_LITERAL = CX_POLICY_NS + "AffiliatesBpnl";

    public static final String CONFIDENTIAL_INFORMATION_MEASURE_LITERAL = CX_POLICY_NS + "ConfidentialInformationMeasures";
    public static final String CONFIDENTIAL_INFORMATION_SHARING_LITERAL = CX_POLICY_NS + "ConfidentialInformationSharing";
    public static final String CONTRACT_REFERENCE_LITERAL = CX_POLICY_NS + "ContractReference";
    public static final String CONTRACT_TERMINATION_LITERAL = CX_POLICY_NS + "ContractTermination";

    public static final String DATA_FREQUENCY_LITERAL = CX_POLICY_NS + "DataFrequency";
    public static final String DATA_PROVISIONING_END_DATE_LITERAL = CX_POLICY_NS + "DataProvisioningEndDate";
    public static final String DATA_PROVISIONING_END_DURATION_LITERAL = CX_POLICY_NS + "DataProvisioningEndDurationDays";
    public static final String DATA_USAGE_END_DATE_LITERAL = CX_POLICY_NS + "DataUsageEndDate";
    public static final String DATA_USAGE_END_DEFINITION_LITERAL = CX_POLICY_NS + "DataUsageEndDefinition";
    public static final String DATA_USAGE_END_DURATION_LITERAL = CX_POLICY_NS + "DataUsageEndDurationDays";

    public static final String EXCLUSIVE_USAGE_LITERAL = CX_POLICY_NS + "ExclusiveUsage";
    public static final String JURISDICTION_LOCATION_LITERAL = CX_POLICY_NS + "JurisdictionLocation";
    public static final String JURISDICTION_LOCATION_REFERENCE_LITERAL = CX_POLICY_NS + "JurisdictionLocationReference";
    public static final String LIABILITY_LITERAL = CX_POLICY_NS + "Liability";
    public static final String MANAGED_LEGAL_ENTITY_BPNL_LITERAL = CX_POLICY_NS + "ManagedLegalEntityBpnl";
    public static final String MANAGED_LEGAL_ENTITY_REGION_LITERAL = CX_POLICY_NS + "ManagedLegalEntityRegion";

    public static final String PRECEDENCE_LITERAL = CX_POLICY_NS + "Precedence";
    public static final String USAGE_RESTRICTION_LITERAL = CX_POLICY_NS + "UsageRestriction";
    public static final String VERSION_CHANGES_LITERAL = CX_POLICY_NS + "VersionChanges";
    public static final String WARRANTY_LITERAL = CX_POLICY_NS + "Warranty";
    public static final String WARRANTY_DEFINITION_LITERAL = CX_POLICY_NS + "WarrantyDefinition";
    public static final String WARRANTY_DURATION_MONTHS_LITERAL = CX_POLICY_NS + "WarrantyDurationMonths";


    // old
    public static final String INFORCE_POLICY_LITERAL = "https://w3id.org/edc/v0.0.1/ns/inForceDate";

    public static final Set<String> NOT_ALLOWED_LOGICAL_CONSTRAINTS = Set.of(
            ODRL_OR_CONSTRAINT_ATTRIBUTE,
            ODRL_XONE_CONSTRAINT_ATTRIBUTE,
            ODRL_AND_SEQUENCE_CONSTRAINT_ATTRIBUTE
    );
    public static final Set<String> ALLOWED_LOGICAL_CONSTRAINTS = Set.of(
            ODRL_AND_CONSTRAINT_ATTRIBUTE
    );
    public static final Set<String> ACCESS_PERMISSION_POLICY_ALLOWED_LEFT_OPERANDS = Set.of(
            FRAMEWORK_AGREEMENT_LITERAL,
            MEMBERSHIP_LITERAL,
            BUSINESS_PARTNER_GROUP_LITERAL,
            BUSINESS_PARTNER_NUMBER_LITERAL,
            INFORCE_POLICY_LITERAL
    );
    public static final Set<String> ACCESS_PROHIBITION_POLICY_ALLOWED_LEFT_OPERANDS = Set.of();
    public static final Set<String> ACCESS_OBLIGATION_POLICY_ALLOWED_LEFT_OPERANDS = Set.of();
    public static final Set<String> USAGE_PERMISSION_POLICY_ALLOWED_LEFT_OPERANDS = Set.of(
            FRAMEWORK_AGREEMENT_LITERAL,
            USAGE_PURPOSE_LITERAL,
            MEMBERSHIP_LITERAL,
            CONTRACT_REFERENCE_LITERAL,
            AFFILIATES_REGION_LITERAL,
            AFFILIATES_BPNL_LITERAL,
            MANAGED_LEGAL_ENTITY_BPNL_LITERAL,
            MANAGED_LEGAL_ENTITY_REGION_LITERAL,
            DATA_FREQUENCY_LITERAL,
            VERSION_CHANGES_LITERAL,
            CONTRACT_TERMINATION_LITERAL,
            CONFIDENTIAL_INFORMATION_MEASURE_LITERAL,
            CONFIDENTIAL_INFORMATION_SHARING_LITERAL,
            EXCLUSIVE_USAGE_LITERAL,
            WARRANTY_LITERAL,
            WARRANTY_DEFINITION_LITERAL,
            WARRANTY_DURATION_MONTHS_LITERAL,
            LIABILITY_LITERAL,
            JURISDICTION_LOCATION_LITERAL,
            JURISDICTION_LOCATION_REFERENCE_LITERAL,
            PRECEDENCE_LITERAL,
            DATA_USAGE_END_DURATION_LITERAL,
            DATA_USAGE_END_DATE_LITERAL,
            DATA_USAGE_END_DEFINITION_LITERAL,
            INFORCE_POLICY_LITERAL
    );
    public static final Set<String> USAGE_PROHIBITION_POLICY_ALLOWED_LEFT_OPERANDS = Set.of(
            AFFILIATES_REGION_LITERAL,
            AFFILIATES_BPNL_LITERAL,
            USAGE_RESTRICTION_LITERAL
    );
    public static final Set<String> USAGE_OBLIGATION_POLICY_ALLOWED_LEFT_OPERANDS = Set.of(
            DATA_PROVISIONING_END_DURATION_LITERAL,
            DATA_PROVISIONING_END_DATE_LITERAL
    );
}
