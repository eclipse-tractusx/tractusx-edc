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

import lombok.Getter;
import lombok.Setter;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;

@Getter
@Setter
public class ContractAgreementData {
  private String id;
  private String providerAgentId;
  private String consumerAgentId;
  private long contractSigningDate;
  private long contractStartDate;
  private long contractEndDate;
  private String assetId;
  private String policyId;

  public static ContractAgreementData from(ContractAgreement agreement) {
    ContractAgreementData data = new ContractAgreementData();
    data.setId(agreement.getId());
    data.setAssetId(agreement.getAssetId());
    data.setContractStartDate(agreement.getContractStartDate());
    data.setContractEndDate(agreement.getContractEndDate());
    data.setContractSigningDate(agreement.getContractSigningDate());
    return data;
  }
}
