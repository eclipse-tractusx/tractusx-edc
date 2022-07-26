package net.catenax.edc.tests.features;

import io.cucumber.java.ParameterType;
import net.catenax.edc.tests.Connector;
import net.catenax.edc.tests.ConnectorFactory;

public class ParameterTypes {

  @ParameterType("Plato|Sokrates")
  public Connector connector(String name) {
    return ConnectorFactory.byName(name);
  }
}
