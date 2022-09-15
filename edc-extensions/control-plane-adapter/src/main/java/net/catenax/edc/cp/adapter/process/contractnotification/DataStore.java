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

package net.catenax.edc.cp.adapter.process.contractnotification;

import net.catenax.edc.cp.adapter.messaging.Message;

public interface DataStore {
  Message exchangeConfirmedContract(String contractNegotiationId, String contractAgreementId);

  Message exchangeDeclinedContract(String contractNegotiationId);

  Message exchangeErrorContract(String contractNegotiationId);

  ContractInfo exchangeMessage(Message message);

  void removeContractInfo(String key);

  void removeMessage(String key);
}
