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
import io.cucumber.java.en.Given;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.tractusx.edc.tests.data.Asset;
import org.eclipse.tractusx.edc.tests.data.NullDataAddress;

public class AssetStepDefs {

  @Given("'{connector}' has the following assets")
  public void hasAssets(Connector connector, DataTable table) throws Exception {
    final DataManagementAPI api = connector.getDataManagementAPI();
    final List<Asset> assets = parseDataTable(table);

    for (Asset asset : assets) api.createAsset(asset);
  }

  @Given("'{connector}' has '{int}' assets")
  public void hasAssets(Connector connector, int assetCount) throws Exception {
    final DataManagementAPI api = connector.getDataManagementAPI();

    for (var i = 0; i < assetCount; i++)
      api.createAsset(
          new Asset(
              UUID.randomUUID().toString(), i + 1 + " / " + assetCount, NullDataAddress.INSTANCE));
  }

  private List<Asset> parseDataTable(DataTable table) {
    final List<Asset> assets = new ArrayList<>();

    for (Map<String, String> map : table.asMaps()) {
      String id = map.get("id");
      String description = map.get("description");
      assets.add(new Asset(id, description, NullDataAddress.INSTANCE));
    }

    return assets;
  }
}
