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

import java.util.ArrayList;
import net.catenax.edc.tests.data.ContractDefinition;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DataManagementApiContractDefinitionMapper {
  DataManagementApiContractDefinitionMapper INSTANCE =
      Mappers.getMapper(DataManagementApiContractDefinitionMapper.class);

  default DataManagementApiContractDefinition map(final ContractDefinition contractDefinition) {
    if (contractDefinition == null) {
      return null;
    }

    final DataManagementApiContractDefinition apiObject = new DataManagementApiContractDefinition();
    apiObject.setId(contractDefinition.getId());
    apiObject.setAccessPolicyId(contractDefinition.getAcccessPolicyId());
    apiObject.setContractPolicyId(contractDefinition.getContractPolicyId());
    apiObject.setCriteria(new ArrayList<>());

    for (final String assetId : contractDefinition.getAssetIds()) {
      DataManagementApiCriterion criterion = new DataManagementApiCriterion();
      criterion.setOperandLeft(DataManagementApiAsset.ID);
      criterion.setOperator("=");
      criterion.setOperandRight(assetId);

      apiObject.getCriteria().add(criterion);
    }

    return apiObject;
  }
}
