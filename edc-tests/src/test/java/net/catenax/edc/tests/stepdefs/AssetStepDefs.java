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
import java.util.UUID;
import lombok.NonNull;
import net.catenax.edc.tests.Connector;
import net.catenax.edc.tests.api.datamanagement.DataManagementApiClient;
import net.catenax.edc.tests.data.Asset;

public class AssetStepDefs {

  @Given("'{connector}' has the following assets")
  public void hasAssets(@NonNull final Connector connector, @NonNull final DataTable table)
      throws Exception {
    final DataManagementApiClient api = connector.getDataManagementApiClient();

    parseDataTable(table).forEach(api::createAsset);
  }

  @Given("'{connector}' has '{int}' assets")
  public void hasAssets(@NonNull final Connector connector, int assetCount) throws Exception {
    final DataManagementApiClient api = connector.getDataManagementApiClient();

    for (var i = 0; i < assetCount; i++)
      api.createAsset(
          Asset.builder()
              .id(UUID.randomUUID().toString())
              .description(i + 1 + " / " + assetCount)
              .build());
  }

  private List<Asset> parseDataTable(DataTable table) {
    final List<Asset> assets = new ArrayList<>();

    for (Map<String, String> map : table.asMaps()) {
      String id = map.get("id");
      String description = map.get("description");
      assets.add(Asset.builder().id(id).description(description).build());
    }

    return assets;
  }
}
