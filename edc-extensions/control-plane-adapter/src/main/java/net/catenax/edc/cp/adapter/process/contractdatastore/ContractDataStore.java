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

import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;

public interface ContractDataStore {
  void add(String assetId, String provider, ContractAgreement contractAgreement);

  ContractAgreementData get(String assetId, String provider);
}
