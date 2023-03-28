package org.eclipse.tractusx.edc.tests.data;

import java.util.List;
import java.util.Objects;


public class OrConstraint implements Constraint {

    private final List<? extends Constraint> constraints;

    public OrConstraint(List<? extends Constraint> constraints) {
        this.constraints = Objects.requireNonNull(constraints);
    }

    public List<? extends Constraint> getConstraints() {
        return constraints;
    }
}
