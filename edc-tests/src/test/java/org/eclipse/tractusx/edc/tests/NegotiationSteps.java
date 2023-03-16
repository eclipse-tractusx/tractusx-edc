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

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.edc.tests.data.ContractNegotiation;
import org.eclipse.tractusx.edc.tests.data.ContractNegotiationState;
import org.eclipse.tractusx.edc.tests.data.Negotiation;
import org.eclipse.tractusx.edc.tests.data.Permission;
import org.eclipse.tractusx.edc.tests.data.Policy;
import org.junit.jupiter.api.Assertions;

@Slf4j
public class NegotiationSteps {

  private static final String DEFINITION_ID = "definition id";
  private static final String ASSET_ID = "asset id";

  private ContractNegotiation lastInitiatedNegotiation;

  @When("'{connector}' sends '{connector}' an offer without constraints")
  public void sendAnOfferWithoutConstraints(Connector sender, Connector receiver, DataTable table)
      throws IOException {

    final DataManagementAPI dataManagementAPI = sender.getDataManagementAPI();
    final String receiverIdsUrl = receiver.getEnvironment().getIdsUrl() + "/data";

    for (Map<String, String> map : table.asMaps()) {
      final String definitionId = map.get(DEFINITION_ID);
      final String assetId = map.get(ASSET_ID);

      final Permission permission = new Permission("USE", null, new ArrayList<>());
      final Policy policy = new Policy("foo", List.of(permission));

      final Negotiation negotiation =
          dataManagementAPI.initiateNegotiation(receiverIdsUrl, definitionId, assetId, policy);

      // wait for negotiation to complete
      negotiation.waitUntilComplete(dataManagementAPI);

      lastInitiatedNegotiation = dataManagementAPI.getNegotiation(negotiation.getId());
    }
  }

  @When("'{connector}' successfully negotiation a contract agreement with '{connector}'")
  public void sokratesSuccessfullyNegotiationAContractAgreementPlatoFor(
      Connector consumer, Connector provider, DataTable table) throws IOException {
    final DataManagementAPI api = consumer.getDataManagementAPI();

    final Map<String, String> map = table.asMap();
    final String definitionId = map.get(DEFINITION_ID);
    final String assetId = map.get(ASSET_ID);

    // as default always the "allow all" policy is used. So we can assume this here, too.
    final Permission permission = new Permission("USE", null, new ArrayList<>());
    final Policy policy = new Policy("policy-id", List.of(permission));

    final String receiverUrl = provider.getEnvironment().getIdsUrl();
    final Negotiation negotiation =
        api.initiateNegotiation(receiverUrl, assetId, definitionId, policy);

    negotiation.waitUntilComplete(api);
  }

  @Then("the negotiation is declined")
  public void assertLastNegotiationDeclined() {
    Assertions.assertEquals(ContractNegotiationState.DECLINED, lastInitiatedNegotiation.getState());
  }
}
