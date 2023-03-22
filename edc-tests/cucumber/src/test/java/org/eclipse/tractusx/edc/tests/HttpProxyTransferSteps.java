package org.eclipse.tractusx.edc.tests;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.eclipse.tractusx.edc.tests.data.*;
import org.junit.jupiter.api.Assertions;

import static org.awaitility.Awaitility.await;

@Slf4j
public class HttpProxyTransferSteps {

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
    final DataManagementAPI api = consumer.getDataManagementAPI();
    final String receiverUrl = provider.getEnvironment().getIdsUrl() + "/data";

    final List<ContractNegotiation> negotiation = api.getNegotiations();
    final String agreementId = negotiation.get(0).getAgreementId();
    final DataAddress dataAddress = new HttpProxySinkDataAddress();

    for (var map : dataTable.asMaps()) {
      final String assetId = map.get(ASSET_ID);
      final String receiverHttpEndpoint = map.get(RECEIVER_HTTP_ENDPOINT);
      final Transfer transfer =
          api.initiateTransferProcess(
              receiverUrl, agreementId, assetId, dataAddress, receiverHttpEndpoint);

      transfer.waitUntilComplete(api);
    }
  }

  @Then("the backend application of '{connector}' has received data")
  public void theBackendApplicationOfSocratesHasReceivedData(Connector consumer) {
    final BackendServiceBackendAPI api = consumer.getBackendServiceBackendAPI();
    await()
            .atMost(Duration.ofSeconds(20))
            .pollInterval(Duration.ofSeconds(1))
            .untilAsserted(() ->{
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
