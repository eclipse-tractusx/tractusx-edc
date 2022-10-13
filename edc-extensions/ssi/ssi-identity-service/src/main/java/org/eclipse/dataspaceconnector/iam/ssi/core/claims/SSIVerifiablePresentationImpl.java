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
 *      ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.iam.ssi.core.claims;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.iam.ssi.model.VerifiableCredentialDto;
import org.eclipse.dataspaceconnector.iam.ssi.model.VerifiablePresentationDto;
import org.eclipse.dataspaceconnector.iam.ssi.model.VerifiablePresentationRequestDto;
import org.eclipse.dataspaceconnector.ssi.spi.IdentityWalletApiService;

import java.util.ArrayList;
import java.util.List;

public class SSIVerifiablePresentationImpl implements SSIVerifiablePresentation {

  IdentityWalletApiService walletApiService;

  public SSIVerifiablePresentationImpl(IdentityWalletApiService walletApiService) {
    this.walletApiService = walletApiService;
  }

  /**
   * Generates a Verifiable Presentation by requesting it from the IdentityWalletAPI Service with
   * a given VerifiableCredentialDto
   * @param vc
   * @return VerifiablePresentationDto
   * @throws JsonProcessingException
   */
  @Override
  public VerifiablePresentationDto getPresentation(VerifiableCredentialDto vc) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    List<VerifiableCredentialDto> credentialDtoList = new ArrayList<>();
    credentialDtoList.add(vc);

    VerifiablePresentationRequestDto vpRequest = new VerifiablePresentationRequestDto(
            walletApiService.getOwnerBPN(),
            credentialDtoList
    );

    String vpRequestJsonString = mapper.writeValueAsString(vpRequest);
    String vpAsString = walletApiService.issueVerifiablePresentation(vpRequestJsonString);
    VerifiablePresentationDto vp = mapper.readValue(vpAsString, VerifiablePresentationDto.class);

    return vp;
  }
}
