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

package org.eclipse.dataspaceconnector.iam.ssi.core;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.iam.ssi.core.claims.*;
import org.eclipse.dataspaceconnector.iam.ssi.model.VerifiablePresentationDto;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.ssi.spi.IdentityWalletApiService;

public class SSIIdentityServiceImpl implements IdentityService {

  private final SSIClaims claims;
  private final SSIVerification verification;

  public SSIIdentityServiceImpl(IdentityWalletApiService walletApiService, SSIVerifiableCredentials verifiableCredentials, SSIVerifiablePresentation verifiablePresentation) {
    claims = new SSIClaims(verifiableCredentials, verifiablePresentation);
    verification = new SSIVerificationImpl(walletApiService);
  }

  /**
   *
   * @param scope the given type of the needed credential
   * @return Tokenrepresentation with the Verifiable Presentation as json string claim
   */
  @Override
  public Result<TokenRepresentation> obtainClientCredentials(String scope) {
    scope = "MembershipCredential";
    TokenRepresentation token;
    try {
      VerifiablePresentationDto vp = claims.getVerifiablePresentation(scope);
      token = claims.makeTokenFromVerifiablePresentation(vp);
      return Result.success(token);
    } catch (Exception e) {
      return Result.failure(e.getMessage());
    }
  }

  /**
   * Verifies the token representation of the json verifiable presentation
   * @param tokenRepresentation A token representation including the token to verify.
   * @return Result<ClaimToken> with the json VerifiablePresentation of the EDC
   */
  @Override
  public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation) {
    ObjectMapper mapper = new ObjectMapper();

    var token = tokenRepresentation.getToken();
    try {
      VerifiablePresentationDto tokenVP = mapper.readValue(token, VerifiablePresentationDto.class);
      if(verification.verifyPresentation(tokenVP)){
        Result<TokenRepresentation> responseToken = obtainClientCredentials("");
        var claimTokenBuilder = ClaimToken.Builder.newInstance();
        claimTokenBuilder.claim("", responseToken.getContent().getToken());
        return Result.success(claimTokenBuilder.build());
      } else{
        return Result.failure("Invalid Token");
      }
    } catch (JsonProcessingException e) {
      return Result.failure(e.getMessage());
    }
  }

  @Override
  public Result<ClaimToken> verifyJwtToken(String token) {
    return IdentityService.super.verifyJwtToken(token);
  }
}