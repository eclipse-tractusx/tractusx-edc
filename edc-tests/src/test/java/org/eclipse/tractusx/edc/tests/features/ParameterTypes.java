package org.eclipse.tractusx.edc.tests.features;

import io.cucumber.java.ParameterType;
import org.eclipse.tractusx.edc.tests.Connector;
import org.eclipse.tractusx.edc.tests.ConnectorFactory;

public class ParameterTypes {

  @ParameterType("Plato|Sokrates")
  public Connector connector(String name) {
    return ConnectorFactory.byName(name);
  }
}
