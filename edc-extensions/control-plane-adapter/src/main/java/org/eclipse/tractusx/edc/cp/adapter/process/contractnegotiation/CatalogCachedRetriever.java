/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 * ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.tractusx.edc.cp.adapter.process.contractnegotiation;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.util.collection.CollectionUtil;
import org.eclipse.tractusx.edc.cp.adapter.util.ExpiringMap;

@RequiredArgsConstructor
public class CatalogCachedRetriever {
  private final CatalogRetriever catalogRetriever;
  private final ExpiringMap<String, Catalog> catalogCache;

  public Catalog getEntireCatalog(String providerUrl, int catalogExpiryTime) {
    return getEntireCatalog(providerUrl, null, catalogExpiryTime);
  }

  public Catalog getEntireCatalog(String providerUrl, String assetId, int catalogExpiryTime) {
    Catalog catalog = catalogCache.get(getKey(providerUrl, assetId), catalogExpiryTime);
    if (Objects.nonNull(catalog)) {
      return catalog;
    }

    catalog = catalogRetriever.getEntireCatalog(providerUrl, assetId);

    if (Objects.nonNull(catalog) && CollectionUtil.isNotEmpty(catalog.getContractOffers())) {
      catalogCache.put(getKey(providerUrl, assetId), catalog);
    }

    return catalog;
  }

  private String getKey(String providerUrl, String assetId) {
    return providerUrl + "::" + assetId;
  }
}
