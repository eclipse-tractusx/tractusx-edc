/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial Implementation
 *
 */

package net.catenax.edc.tests;

import static org.awaitility.Awaitility.await;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import net.catenax.edc.tests.data.ContractNegotiation;
import net.catenax.edc.tests.data.ContractNegotiationState;
import net.catenax.edc.tests.data.Permission;
import net.catenax.edc.tests.data.Policy;
import net.catenax.edc.tests.util.Timeouts;
import org.junit.jupiter.api.Assertions;

@Slf4j
public class NegotiationSteps {

  private ContractNegotiation lastInitiatedNegotiation;

  @When("'{connector}' sends '{connector}' a counter offer without constraints")
  public void sendOfferWithoutConstraints(Connector sender, Connector receiver, DataTable table)
      throws IOException {

    final DataManagementAPI dataManagementAPI = sender.getDataManagementAPI();
    final String receiverIdsUrl = receiver.getEnvironment().getIdsUrl() + "/data";

    for (Map<String, String> map : table.asMaps()) {
      final String definitionId = map.get("definition id");
      final String assetId = map.get("asset id");

      final Permission permission = new Permission("USE", null, new ArrayList<>());
      final Policy policy = new Policy("foo", List.of(permission));

      final String negotiationId =
          dataManagementAPI.initiateNegotiation(receiverIdsUrl, definitionId, assetId, policy);

      // wait for negotiation to complete
      await()
          .pollDelay(Duration.ofMillis(500))
          .atMost(Timeouts.CONTRACT_NEGOTIATION)
          .until(() -> isNegotiationComplete(dataManagementAPI, negotiationId));

      lastInitiatedNegotiation = dataManagementAPI.getNegotiation(negotiationId);
    }
  }

  @Then("the negotiation is declined")
  public void assertLastNegotiationDeclined() {
    Assertions.assertEquals(ContractNegotiationState.DECLINED, lastInitiatedNegotiation.getState());
  }

  private boolean isNegotiationComplete(DataManagementAPI dataManagementAPI, String negotiationId)
      throws IOException {
    var negotiation = dataManagementAPI.getNegotiation(negotiationId);
    return negotiation != null
        && Stream.of(
                ContractNegotiationState.ERROR,
                ContractNegotiationState.CONFIRMED,
                ContractNegotiationState.DECLINED)
            .anyMatch((l) -> l.equals(negotiation.getState()));
  }
}
