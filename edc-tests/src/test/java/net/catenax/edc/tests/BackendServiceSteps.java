package net.catenax.edc.tests;

import io.cucumber.java.en.Given;

public class BackendServiceSteps {

  @Given("'{connector}' has an empty backend-service")
  public void cleanBackendService(Connector connector) {
    final BackendServiceBackendAPI backendServiceBackendAPI =
        connector.getBackendServiceBackendAPI();

    backendServiceBackendAPI.list("/").forEach(backendServiceBackendAPI::delete);
  }
}
