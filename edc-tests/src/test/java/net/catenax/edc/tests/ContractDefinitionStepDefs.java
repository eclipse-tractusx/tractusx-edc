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

package net.catenax.edc.tests;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.catenax.edc.tests.data.ContractDefinition;

public class ContractDefinitionStepDefs {

  @Given("'{connector}' has no contract definitions")
  public void hasNoContractDefinitions(Connector connector) throws Exception {

    final DataManagementAPI api = connector.getDataManagementAPI();

    Stream<ContractDefinition> contractDefinitions = api.getAllContractDefinitions();
    for (ContractDefinition contractDefinition :
        contractDefinitions.toArray(ContractDefinition[]::new)) {
      api.deleteContractDefinition(contractDefinition.getId());
    }
  }

  @Given("'{connector}' has the following contract definitions")
  public void hasPolicies(Connector connector, DataTable table) throws Exception {
    final DataManagementAPI api = connector.getDataManagementAPI();
    final List<ContractDefinition> contractDefinitions = parseDataTable(table);

    for (ContractDefinition contractDefinition : contractDefinitions)
      api.createContractDefinition(contractDefinition);
  }

  private List<ContractDefinition> parseDataTable(DataTable table) {
    final List<ContractDefinition> contractDefinitions = new ArrayList<>();

    for (Map<String, String> map : table.asMaps()) {
      String id = map.get("id");
      String accessPolicyId = map.get("access policy");
      String contractPolicyId = map.get("contract policy");
      String assetid = map.get("asset");
      contractDefinitions.add(
          new ContractDefinition(id, contractPolicyId, accessPolicyId, List.of(assetid)));
    }

    return contractDefinitions;
  }
}
