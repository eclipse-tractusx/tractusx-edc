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

import java.util.Map;
import net.catenax.edc.tests.data.Asset;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DataManagementApiAssetMapper {
  DataManagementApiAssetMapper INSTANCE = Mappers.getMapper(DataManagementApiAssetMapper.class);

  default DataManagementApiAsset map(Asset asset) {
    if (asset == null) {
      return null;
    }

    final Map<String, Object> properties =
        Map.of(
            DataManagementApiAsset.ID, asset.getId(),
            DataManagementApiAsset.DESCRIPTION, asset.getDescription());

    final DataManagementApiAsset apiObject = new DataManagementApiAsset();
    apiObject.setProperties(properties);
    return apiObject;
  }
}
