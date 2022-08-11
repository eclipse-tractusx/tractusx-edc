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

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.catenax.edc.tests.data.Catalog;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DataManagementApiContractOfferCatalogMapper {
  DataManagementApiContractOfferCatalogMapper INSTANCE =
      Mappers.getMapper(DataManagementApiContractOfferCatalogMapper.class);

  default Catalog map(
      final DataManagementApiContractOfferCatalog dataManagementApiContractOfferCatalog) {
    if (dataManagementApiContractOfferCatalog == null) {
      return null;
    }

    return Catalog.builder()
        .id(dataManagementApiContractOfferCatalog.getId())
        .contractOffers(
            Optional.ofNullable(dataManagementApiContractOfferCatalog.getContractOffers())
                .orElseGet(Collections::emptyList)
                .stream()
                .map(DataManagementApiContractOfferMapper.INSTANCE::map)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()))
        .build();
  }
}
