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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.stream.Stream;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ContractAgreementRetrieverTest {
  @Mock Monitor monitor;
  @Mock ContractAgreementService agreementService;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void getExistingContractByAssetId_shouldReturnValidContract() {
    // given
    long now = Instant.now().getEpochSecond();
    when(agreementService.query(any())).thenReturn(getResult(now + 1000));
    ContractAgreementRetriever retriever =
        new ContractAgreementRetriever(monitor, agreementService);

    // when
    ContractAgreement contractAgreement = retriever.getExistingContractByAssetId("id");

    // then
    Assertions.assertNotNull(contractAgreement);
  }

  @Test
  public void getExistingContractByAssetId_shouldNotReturnExpiredContract() {
    // given
    long now = Instant.now().getEpochSecond();
    when(agreementService.query(any())).thenReturn(getResult(now - 1000));
    ContractAgreementRetriever retriever =
        new ContractAgreementRetriever(monitor, agreementService);

    // when
    ContractAgreement contractAgreement = retriever.getExistingContractByAssetId("id");

    // then
    Assertions.assertNull(contractAgreement);
  }

  private ServiceResult<Stream<ContractAgreement>> getResult(long endDate) {
    long now = Instant.now().getEpochSecond();
    return ServiceResult.success(
        Stream.of(
            ContractAgreement.Builder.newInstance()
                .id("id")
                .assetId("assetId")
                .contractStartDate(now - 2000)
                .contractEndDate(endDate)
                .providerAgentId("providerId")
                .consumerAgentId("consumerId")
                .policy(Policy.Builder.newInstance().build())
                .build()));
  }
}
