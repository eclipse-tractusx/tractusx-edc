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

package net.catenax.edc.tests.api.datamanagement;

import net.catenax.edc.tests.data.Asset;
import net.catenax.edc.tests.data.Catalog;
import net.catenax.edc.tests.data.ContractDefinition;
import net.catenax.edc.tests.data.ContractNegotiation;
import net.catenax.edc.tests.data.Policy;

public interface DataManagementApiClient {
  Catalog getCatalog(String receivingConnectorUrl);

  ContractNegotiation initiateNegotiation(
      String receivingConnectorUrl, String definitionId, String assetId, Policy policy);

  ContractNegotiation getNegotiation(String id);

  void createAsset(Asset asset);

  void createPolicy(Policy policy);

  void createContractDefinition(ContractDefinition contractDefinition);
}
