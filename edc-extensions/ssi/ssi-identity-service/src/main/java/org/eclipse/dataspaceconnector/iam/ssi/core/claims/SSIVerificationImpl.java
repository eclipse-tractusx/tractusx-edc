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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.iam.ssi.core.did.SSIDidResolver;
import org.eclipse.dataspaceconnector.iam.ssi.core.did.SSIDidResolverImpl;
import org.eclipse.dataspaceconnector.iam.ssi.model.VerifiablePresentationDto;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.ssi.spi.IdentityWalletApiService;


/**
 * Verification of the signature validation from a given Verifiable Presentation
 */
public class SSIVerificationImpl implements SSIVerification {

  private final SSIDidResolver didResolver;
  private final IdentityWalletApiService walletApiService;

  public SSIVerificationImpl(IdentityWalletApiService walletApiService) {
    didResolver = new SSIDidResolverImpl(walletApiService);
    this.walletApiService = walletApiService;
  }


  /**
   * Verification of a given Presentation by consuming the validation
   * Service Endpoint of the Wallet Identity Service
   * @param vp
   * @return
   */
  @Override
  public boolean verifyPresentation(VerifiablePresentationDto vp) {
    boolean result = false;
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      String jsonVP = mapper.writeValueAsString(vp);
      String validationResult = walletApiService.validateVerifablePresentation(jsonVP);
      if(!validationResult.equals(null)){
        result = true;
      }
    } catch (Exception e) {
      throw new EdcException(e.getMessage());
    }
    return result;
  }
}
