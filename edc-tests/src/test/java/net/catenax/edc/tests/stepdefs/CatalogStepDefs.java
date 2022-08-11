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
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package net.catenax.edc.tests.stepdefs;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import net.catenax.edc.tests.Connector;
import net.catenax.edc.tests.api.datamanagement.DataManagementApiClient;
import net.catenax.edc.tests.data.Catalog;
import net.catenax.edc.tests.data.ContractOffer;
import org.junit.jupiter.api.Assertions;

public class CatalogStepDefs {

  private Catalog catalog;

  @When("'{connector}' requests the catalog from '{connector}'")
  public void requestCatalog(@NonNull final Connector sender, @NonNull final Connector receiver)
      throws IOException {

    final DataManagementApiClient dataManagementAPI = sender.getDataManagementApiClient();
    final String receiverIdsUrl = receiver.getEnvironment().getIdsUrl() + "/data";

    catalog = dataManagementAPI.getCatalog(receiverIdsUrl);
  }

  @Then("the catalog contains the following offers")
  public void verifyCatalogContains(@NonNull final DataTable table) {
    for (Map<String, String> map : table.asMaps()) {
      final String sourceContractDefinitionId = map.get("source definition");
      final String assetId = map.get("asset");

      final boolean isInCatalog = isInCatalog(assetId, sourceContractDefinitionId);

      Assertions.assertTrue(
          isInCatalog,
          String.format(
              "Expected the catalog to contain offer for definition '%s' and asset '%s' ",
              sourceContractDefinitionId, assetId));
    }
  }

  @Then("the catalog does not contain the following offers")
  public void verifyCatalogContainsNot(@NonNull final DataTable table) {
    for (Map<String, String> map : table.asMaps()) {
      final String sourceContractDefinitionId = map.get("source definition");
      final String assetId = map.get("asset");

      final boolean isInCatalog = isInCatalog(assetId, sourceContractDefinitionId);

      Assertions.assertFalse(
          isInCatalog,
          String.format(
              "Expected the catalog to not contain offer for definition '%s' and asset '%s' ",
              sourceContractDefinitionId, assetId));
    }
  }

  @Then("the catalog contains '{int}' offers")
  public void verifyCatalogContainsXOffers(int offerCount) {
    Assertions.assertEquals(
        offerCount,
        getContractOffers(catalog).size(),
        String.format(
            "Expected the catalog to contain '%s' offers, but got '%s'.",
            offerCount, getContractOffers(catalog).size()));
  }

  private List<ContractOffer> getContractOffers(final Catalog catalog) {
    return Optional.ofNullable(catalog)
        .map(Catalog::getContractOffers)
        .orElseGet(Collections::emptyList);
  }

  private boolean isInCatalog(final String assetId, final String definitionId) {
    return getContractOffers(catalog).stream()
        .anyMatch(c -> c.getAssetId().equals(assetId) && c.getId().startsWith(definitionId));
  }
}
