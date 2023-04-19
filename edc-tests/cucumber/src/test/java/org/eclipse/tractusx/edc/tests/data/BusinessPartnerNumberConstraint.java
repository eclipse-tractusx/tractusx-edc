package org.eclipse.tractusx.edc.tests.data;

import java.util.Objects;

public class BusinessPartnerNumberConstraint implements Constraint {

    private final String businessPartnerNumber;

    public BusinessPartnerNumberConstraint(String businessPartnerNumber) {
        this.businessPartnerNumber = Objects.requireNonNull(businessPartnerNumber);
    }

    public String getBusinessPartnerNumber() {
        return businessPartnerNumber;
    }
}
