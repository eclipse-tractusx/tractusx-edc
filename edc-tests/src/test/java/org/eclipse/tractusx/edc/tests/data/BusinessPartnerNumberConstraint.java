package org.eclipse.tractusx.edc.tests.data;

import lombok.NonNull;
import lombok.Value;

@Value
public class BusinessPartnerNumberConstraint implements Constraint {

  @NonNull String businessPartnerNumber;
}
