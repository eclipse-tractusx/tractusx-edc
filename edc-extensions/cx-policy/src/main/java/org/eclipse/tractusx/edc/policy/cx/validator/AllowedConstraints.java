package org.eclipse.tractusx.edc.policy.cx.validator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.*;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;

public interface AllowedConstraints {
    String ACCESS_POLICY_TYPE = EDC_NAMESPACE + "access";
    String USAGE_POLICY_TYPE = ODRL_SCHEMA + "use";
    Set<String> NOT_ALLOWED_LOGICAL_CONSTRAINTS = Set.of(
            ODRL_XONE_CONSTRAINT_ATTRIBUTE,
            ODRL_OR_CONSTRAINT_ATTRIBUTE,
            ODRL_AND_SEQUENCE_CONSTRAINT_ATTRIBUTE
    );

    Set<String> ALLOWED_LOGICAL_CONSTRAINTS = Set.of(
            ODRL_AND_CONSTRAINT_ATTRIBUTE
    );

    Set<String> ACCESS_POLICY_ALLOWED_LEFT_OPERANDS = Set.of(
            CX_POLICY_NS + "FrameworkAgreement",
            CX_POLICY_NS + "Membership",
            CX_POLICY_NS + "BusinessPartnerGroup",
            CX_POLICY_NS + "BusinessPartnerNumber"
    );

    Set<String> USAGE_POLICY_ALLOWED_LEFT_OPERANDS = Set.of(
            CX_POLICY_NS + "UsagePurpose",
            CX_POLICY_NS + "Membership",
            CX_POLICY_NS + "AffiliatesRegion",
            CX_POLICY_NS + "FrameworkAgreement"
    );
}
