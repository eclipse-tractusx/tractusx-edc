package org.eclipse.tractusx.edc.tests;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.eclipse.tractusx.edc.tests.data.Asset;
import org.eclipse.tractusx.edc.tests.data.DataAddress;
import org.eclipse.tractusx.edc.tests.data.HttpProxySinkDataAddress;
import org.eclipse.tractusx.edc.tests.data.HttpProxySourceDataAddress;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class HttpProxyTransferSteps {

    private static final Logger log = LoggerFactory.getLogger(HttpProxyTransferSteps.class);

    private static final String ID = "id";
    private static final String DESCRIPTION = "description";
    private static final String BASE_URL = "baseUrl";
    private static final String ASSET_ID = "asset id";
    private static final String RECEIVER_HTTP_ENDPOINT = "receiverHttpEndpoint";

    @Given("'{connector}' has a http proxy assets")
    public void hasAssets(Connector connector, DataTable table) throws Exception {
        final DataManagementAPI api = connector.getDataManagementAPI();

        for (var map : table.asMaps()) {
            final String id = map.get(ID);
            final String description = map.get(DESCRIPTION);
            final String baseUrl = map.get(BASE_URL);

            var oauth2Provision =
                    Arrays.stream(Oauth2DataAddressFields.values())
                            .map(it -> it.text)
                            .anyMatch(map::containsKey)
                            ? new HttpProxySourceDataAddress.Oauth2Provision(
                            map.get(Oauth2DataAddressFields.TOKEN_URL.text),
                            map.get(Oauth2DataAddressFields.CLIENT_ID.text),
                            map.get(Oauth2DataAddressFields.CLIENT_SECRET.text),
                            map.get(Oauth2DataAddressFields.SCOPE.text))
                            : null;

            final DataAddress address = new HttpProxySourceDataAddress(baseUrl, oauth2Provision);
            final Asset asset = new Asset(id, description, address);

            api.createAsset(asset);
        }
    }

    @When("'{connector}' initiates HttpProxy transfer from '{connector}'")
    public void sokratesInitiateHttpProxyTransferProcessFromPlato(
            Connector consumer, Connector provider, DataTable dataTable) throws IOException {
        var api = consumer.getDataManagementAPI();
        var receiverUrl = provider.getEnvironment().getIdsUrl() + "/data";

        var negotiation = api.getNegotiations();
        var agreementId = negotiation.get(0).getAgreementId();
        var dataAddress = new HttpProxySinkDataAddress();

        for (var map : dataTable.asMaps()) {
            final String assetId = map.get(ASSET_ID);
            final String receiverHttpEndpoint = map.get(RECEIVER_HTTP_ENDPOINT);
            var transfer = api.initiateTransferProcess(receiverUrl, agreementId, assetId, dataAddress, receiverHttpEndpoint);

            transfer.waitUntilComplete(api);
        }
    }

    @Then("the backend application of '{connector}' has received data")
    public void theBackendApplicationOfSocratesHasReceivedData(Connector consumer) {
        var api = consumer.getBackendServiceBackendAPI();
        when(api.list(eq("/"))).thenReturn(List.of("item1", "item2"));
        await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    final List<String> transferredData = api.list("/");
                    Assertions.assertNotEquals(0, transferredData.size());
                });
    }

    private enum Oauth2DataAddressFields {
        TOKEN_URL("oauth2 token url"),
        CLIENT_ID("oauth2 client id"),
        CLIENT_SECRET("oauth2 client secret"),
        SCOPE("oauth2 scope");

        private final String text;

        Oauth2DataAddressFields(String text) {
            this.text = text;
        }
    }
}
