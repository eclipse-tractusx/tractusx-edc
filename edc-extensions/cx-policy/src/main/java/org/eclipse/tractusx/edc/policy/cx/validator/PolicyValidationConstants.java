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

import java.util.Map;
import java.util.Set;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_AND_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_AND_SEQUENCE_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OR_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_XONE_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;

public final class PolicyValidationConstants {
    private PolicyValidationConstants() {}

    public static final String ACTION_ACCESS = CX_POLICY_NS + "access";
    public static final String ACTION_USAGE = ODRL_SCHEMA + "use";

    public static final String ACCESS_POLICY_TYPE = CX_POLICY_NS + "access";
    public static final String USAGE_POLICY_TYPE = ODRL_SCHEMA + "use";

    public static final String FRAMEWORK_AGREEMENT_LITERAL = CX_POLICY_NS + "FrameworkAgreement";
    public static final String MEMBERSHIP_LITERAL = CX_POLICY_NS + "Membership";
    public static final String BUSINESS_PARTNER_GROUP_LITERAL = TX_NAMESPACE + "BusinessPartnerGroup";
    public static final String BUSINESS_PARTNER_NUMBER_LITERAL = TX_NAMESPACE + "BusinessPartnerNumber";
    public static final String USAGE_PURPOSE_LITERAL = CX_POLICY_NS + "UsagePurpose";
    public static final String AFFILIATES_REGION_LITERAL = CX_POLICY_NS + "AffiliatesRegion";

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
            USAGE_PURPOSE_LITERAL,
            MEMBERSHIP_LITERAL,
            AFFILIATES_REGION_LITERAL,
            INFORCE_POLICY_LITERAL
    );
    public static final Set<String> USAGE_PROHIBITION_POLICY_ALLOWED_LEFT_OPERANDS = Set.of(
            USAGE_PURPOSE_LITERAL
    );
    public static final Set<String> USAGE_OBLIGATION_POLICY_ALLOWED_LEFT_OPERANDS = Set.of(
            AFFILIATES_REGION_LITERAL
    );

    public static final Map<String, Set<String>> MUTUALLY_EXCLUSIVE_CONSTRAINTS = Map.of(
            USAGE_PURPOSE_LITERAL, Set.of(INFORCE_POLICY_LITERAL, MEMBERSHIP_LITERAL),
            INFORCE_POLICY_LITERAL, Set.of(USAGE_PURPOSE_LITERAL)
    );

}
