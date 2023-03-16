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

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;

@RequiredArgsConstructor
public class ContractAgreementRetriever {
  private final Monitor monitor;
  private final ContractAgreementService agreementService;

  public ContractAgreement getExistingContractById(String contractAgreementId) {
    return agreementService.findById(contractAgreementId);
  }

  public ContractAgreement getExistingContractByAssetId(String assetId) {
    Collection<ContractAgreement> agreements = getContractAgreementsByAssetId(assetId);

    validateResults(agreements);

    long now = Instant.now().getEpochSecond();
    return agreements.stream()
        .filter(agreement -> agreement.getContractStartDate() < now)
        .filter(agreement -> agreement.getContractEndDate() > now)
        .findFirst()
        .orElse(null);
  }

  private Collection<ContractAgreement> getContractAgreementsByAssetId(String assetId) {
    QuerySpec querySpec =
        QuerySpec.Builder.newInstance()
            .filter(List.of(new Criterion("policy.target", "=", assetId)))
            .limit(500)
            .build();
    ServiceResult<Stream<ContractAgreement>> result = agreementService.query(querySpec);
    return result.succeeded()
        ? result.getContent().collect(Collectors.toList())
        : Collections.emptyList();
  }

  private void validateResults(Collection<ContractAgreement> agreements) {
    if (agreements.size() > 1) {
      monitor.warning(
          "More than one agreement found for a given assetId! First of the list will be used!");
    }
    int numberOfProviders =
        agreements.stream()
            .collect(Collectors.groupingBy(ContractAgreement::getProviderAgentId))
            .size();
    if (numberOfProviders > 1) {
      monitor.warning("Contract agreement: given assetId found for more than one provider!");
    }
  }
}
