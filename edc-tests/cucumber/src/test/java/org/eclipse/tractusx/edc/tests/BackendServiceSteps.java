package org.eclipse.tractusx.edc.tests;

import io.cucumber.java.en.Given;

public class BackendServiceSteps {

    @Given("'{connector}' has an empty backend-service")
    public void cleanBackendService(Connector connector) {
        var backendServiceBackendAPI = connector.getBackendServiceBackendAPI();

        backendServiceBackendAPI.list("/").forEach(backendServiceBackendAPI::delete);
    }
}
