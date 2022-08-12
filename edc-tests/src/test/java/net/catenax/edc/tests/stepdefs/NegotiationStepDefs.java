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

package net.catenax.edc.tests.stepdefs;

import static org.awaitility.Awaitility.await;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.catenax.edc.tests.Connector;
import net.catenax.edc.tests.api.backendservice.BackendServiceBackendApiClient;
import net.catenax.edc.tests.api.datamanagement.DataManagementApiClient;
import net.catenax.edc.tests.data.ContractNegotiation;
import net.catenax.edc.tests.data.ContractNegotiationState;
import net.catenax.edc.tests.data.Permission;
import net.catenax.edc.tests.data.Policy;
import net.catenax.edc.tests.data.TransferProcess;
import net.catenax.edc.tests.util.Timeouts;
import org.junit.jupiter.api.Assertions;

@Slf4j
public class NegotiationStepDefs {

  private ContractNegotiation lastInitiatedNegotiation;
  private String transferId;

  @When("'{connector}' sends '{connector}' a counter offer without constraints")
  public void sendOfferWithoutConstraints(
      @NonNull final Connector sender,
      @NonNull final Connector receiver,
      @NonNull final DataTable table) {

    final DataManagementApiClient dataManagementAPI = sender.getDataManagementApiClient();
    final String receiverIdsUrl = receiver.getEnvironment().getIdsUrl() + "/data";

    for (Map<String, String> map : table.asMaps()) {
      final String definitionId = map.get("definition id");
      final String assetId = map.get("asset id");

      final Permission permission =
          Permission.builder().action("USE").constraints(Collections.emptyList()).build();
      final Policy policy = Policy.builder().id("foo").permission(List.of(permission)).build();

      lastInitiatedNegotiation =
          dataManagementAPI.initiateNegotiation(receiverIdsUrl, definitionId, assetId, policy);

      // wait for negotiation to complete
      await()
          .pollDelay(Duration.ofMillis(500))
          .atMost(Timeouts.CONTRACT_NEGOTIATION)
          .until(() -> isNegotiationComplete(dataManagementAPI, lastInitiatedNegotiation.getId()));

      lastInitiatedNegotiation = dataManagementAPI.getNegotiation(lastInitiatedNegotiation.getId());
    }
  }

  @Then("the negotiation is confirmed")
  public void assertLastNegotiationAccepted() {
    Assertions.assertEquals(
        ContractNegotiationState.CONFIRMED, lastInitiatedNegotiation.getState());
  }

  @Then("'{connector}' starts the transfer process with '{connector}' of asset '{word}'")
  public void startTransferProcess(
      @NonNull final Connector sender,
      @NonNull final Connector receiver,
      @NonNull final String assetId) {
    final DataManagementApiClient dataManagementAPI = sender.getDataManagementApiClient();

    final String receiverIdsUrl = receiver.getEnvironment().getIdsUrl() + "/data";

    final TransferProcess transferProcess =
        TransferProcess.builder()
            .assetId(assetId)
            .contractId(lastInitiatedNegotiation.getAgreementId())
            .id(UUID.randomUUID().toString())
            .type("HttpProxy")
            .connectorAddress(receiverIdsUrl)
            .build();

    transferId = dataManagementAPI.initiateTransferProcess(transferProcess);
  }

  @Then("'{connector}' has file transferred to its backend")
  public void hasFile(@NonNull final Connector sender) {
    final BackendServiceBackendApiClient client = sender.getBackendServiceBackendApiClient();

    // wait for negotiation to complete
    await()
        .pollDelay(Duration.ofMillis(500))
        .atMost(Timeouts.CONTRACT_NEGOTIATION)
        .until(() -> hasFile(client, transferId));
  }

  @Then("the negotiation is declined")
  public void assertLastNegotiationDeclined() {
    Assertions.assertEquals(ContractNegotiationState.DECLINED, lastInitiatedNegotiation.getState());
  }

  private boolean hasFile(
      @NonNull final BackendServiceBackendApiClient backendServiceBackendApiClient,
      @NonNull final String transferId) {
    return backendServiceBackendApiClient.exists("/" + transferId);
  }

  private boolean isNegotiationComplete(
      @NonNull final DataManagementApiClient dataManagementAPI,
      @NonNull final String negotiationId) {
    var negotiation = dataManagementAPI.getNegotiation(negotiationId);
    return negotiation != null
        && Stream.of(
                ContractNegotiationState.ERROR,
                ContractNegotiationState.CONFIRMED,
                ContractNegotiationState.DECLINED)
            .anyMatch((l) -> l.equals(negotiation.getState()));
  }
}
