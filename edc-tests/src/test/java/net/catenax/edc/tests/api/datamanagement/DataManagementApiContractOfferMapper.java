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

import net.catenax.edc.tests.data.ContractOffer;
import net.catenax.edc.tests.data.Policy;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DataManagementApiContractOfferMapper {
  DataManagementApiContractOfferMapper INSTANCE =
      Mappers.getMapper(DataManagementApiContractOfferMapper.class);

  default ContractOffer map(DataManagementApiContractOffer dataManagementApiContractOffer) {
    if (dataManagementApiContractOffer == null) {
      return null;
    }

    final String id = dataManagementApiContractOffer.getId();
    final String assetId =
        dataManagementApiContractOffer.getAssetId() != null
            ? dataManagementApiContractOffer.getAssetId()
            : (String)
                dataManagementApiContractOffer
                    .getAsset()
                    .getProperties()
                    .get(DataManagementApiAsset.ID);

    final Policy policy =
        DataManagementApiPolicyMapper.INSTANCE.map(dataManagementApiContractOffer.getPolicy());

    return ContractOffer.builder().id(id).policy(policy).assetId(assetId).build();
  }
}
