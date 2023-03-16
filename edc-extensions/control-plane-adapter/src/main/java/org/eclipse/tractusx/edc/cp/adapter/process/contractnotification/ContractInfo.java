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

package org.eclipse.tractusx.edc.cp.adapter.process.contractnotification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContractInfo {
  @Getter private String contractAgreementId;
  private ContractState contractState;

  public ContractInfo(String contractAgreementId, ContractState contractState) {
    this.contractAgreementId = contractAgreementId;
    this.contractState = contractState;
  }

  public ContractInfo(ContractState contractState) {
    this.contractState = contractState;
  }

  public boolean isConfirmed() {
    return ContractState.CONFIRMED.equals(contractState);
  }

  public boolean isDeclined() {
    return ContractState.DECLINED.equals(contractState);
  }

  public boolean isError() {
    return ContractState.ERROR.equals(contractState);
  }

  protected enum ContractState {
    CONFIRMED,
    DECLINED,
    ERROR;
  }
}
