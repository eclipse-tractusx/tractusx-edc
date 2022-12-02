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

package net.catenax.edc.cp.adapter.process.contractdatastore;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;

public class InMemoryContractDataStore implements ContractDataStore {
  private static final Map<String, ContractAgreementData> contractMap = new HashMap<>();

  @Override
  public void add(String assetId, String provider, ContractAgreement agreement) {
    contractMap.put(getKey(assetId, provider), ContractAgreementData.from(agreement));
  }

  @Override
  public ContractAgreementData get(String assetId, String provider) {
    return contractMap.get(getKey(assetId, provider));
  }

  private String getKey(String assetId, String provider) {
    return assetId + "::" + provider;
  }
}
