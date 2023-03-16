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

import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;

public interface ContractNotificationSyncService {
  DataReferenceRetrievalDto exchangeConfirmedContract(
      String contractNegotiationId, String contractAgreementId);

  DataReferenceRetrievalDto exchangeDeclinedContract(String contractNegotiationId);

  DataReferenceRetrievalDto exchangeErrorContract(String contractNegotiationId);

  ContractInfo exchangeDto(DataReferenceRetrievalDto dto);

  void removeContractInfo(String key);

  void removeDto(String key);
}
