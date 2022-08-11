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
import io.cucumber.java.en.Given;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import net.catenax.edc.tests.Connector;
import net.catenax.edc.tests.api.datamanagement.DataManagementApiClient;
import net.catenax.edc.tests.data.ContractDefinition;

public class ContractDefinitionStepDefs {

  @Given("'{connector}' has the following contract definitions")
  public void hasPolicies(@NonNull final Connector connector, @NonNull final DataTable table) {
    final DataManagementApiClient api = connector.getDataManagementApiClient();
    parseDataTable(table).forEach(api::createContractDefinition);
  }

  private List<ContractDefinition> parseDataTable(@NonNull final DataTable table) {
    final List<ContractDefinition> contractDefinitions = new ArrayList<>();

    for (Map<String, String> map : table.asMaps()) {
      String id = map.get("id");
      String accessPolicyId = map.get("access policy");
      String contractPolicyId = map.get("contract policy");
      String assetId = map.get("asset");
      List<String> assetIds = assetId == null ? new ArrayList<>() : List.of(assetId);

      contractDefinitions.add(
          ContractDefinition.builder()
              .id(id)
              .contractPolicyId(contractPolicyId)
              .acccessPolicyId(accessPolicyId)
              .assetIds(assetIds)
              .build());
    }

    return contractDefinitions;
  }
}
