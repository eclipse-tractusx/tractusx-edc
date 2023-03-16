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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CatalogRetrieverTest {
  @Mock CatalogService catalogService;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void getEntireCatalog_shouldReturnEntireCatalogIfMoreThanOnePage() {
    // given
    CatalogRetriever catalogRetriever = new CatalogRetriever(50, catalogService);
    when(catalogService.getByProviderUrl(anyString(), any()))
        .thenReturn(getCatalogResult(50), getCatalogResult(50), getCatalogResult(10));

    // when
    Catalog catalog = catalogRetriever.getEntireCatalog("providerUrl", "assetId");

    // then
    assertEquals(110, catalog.getContractOffers().size());
  }

  @Test
  public void getEntireCatalog_shouldEmptyCatalogIfNoResults() {
    // given
    CatalogRetriever catalogRetriever = new CatalogRetriever(50, catalogService);
    when(catalogService.getByProviderUrl(anyString(), any())).thenReturn(getCatalogResult(0));

    // when
    Catalog catalog = catalogRetriever.getEntireCatalog("providerUrl", "assetId");

    // then
    assertEquals(0, catalog.getContractOffers().size());
  }

  private CompletableFuture<Catalog> getCatalogResult(int offersNumber) {
    List<ContractOffer> contractOffers =
        IntStream.range(0, offersNumber)
            .mapToObj(operand -> getContractOffer())
            .collect(Collectors.toList());

    return CompletableFuture.completedFuture(
        Catalog.Builder.newInstance().id("id").contractOffers(contractOffers).build());
  }

  private ContractOffer getContractOffer() {
    Asset asset = Asset.Builder.newInstance().id("assetId").build();
    return ContractOffer.Builder.newInstance()
        .id("id")
        .asset(asset)
        .policy(Policy.Builder.newInstance().build())
        .contractStart(ZonedDateTime.now())
        .contractEnd(ZonedDateTime.now().plusDays(1))
        .build();
  }
}
