/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.tests;

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
import org.eclipse.tractusx.edc.tests.data.ContractNegotiation;
import org.eclipse.tractusx.edc.tests.data.ContractNegotiationState;
import org.eclipse.tractusx.edc.tests.data.Permission;
import org.eclipse.tractusx.edc.tests.data.Policy;
import org.eclipse.tractusx.edc.tests.util.Timeouts;
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

  static boolean isNegotiationComplete(DataManagementAPI dataManagementAPI, String negotiationId)
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
