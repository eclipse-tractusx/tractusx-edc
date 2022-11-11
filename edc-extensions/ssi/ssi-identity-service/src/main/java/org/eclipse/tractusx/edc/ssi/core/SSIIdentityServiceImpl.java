/*
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.edc.ssi.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenParameters;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.tractusx.edc.ssi.core.claims.*;
import org.eclipse.tractusx.edc.ssi.miw.model.VerifiablePresentationDto;
import org.eclipse.tractusx.edc.ssi.spi.IdentityWalletApiService;

public class SSIIdentityServiceImpl implements IdentityService {

  private final SSIClaims claims;
  private final SSIVerification verification;

  public SSIIdentityServiceImpl(
      IdentityWalletApiService walletApiService,
      SSIVerifiableCredentials verifiableCredentials,
      SSIVerifiablePresentation verifiablePresentation) {
    claims = new SSIClaims(verifiableCredentials, verifiablePresentation);
    verification = new SSIVerificationImpl(walletApiService);
  }

  /**
   * @param tokenParameters the given type of the needed credential
   * @return Tokenrepresentation with the Verifiable Presentation as json string claim
   */
  @Override
  public Result<TokenRepresentation> obtainClientCredentials(TokenParameters tokenParameters) {
    var scope = "MembershipCredential";
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
   *
   * @param tokenRepresentation A token representation including the token to verify.
   * @return Result<ClaimToken> with the json VerifiablePresentation of the EDC
   */
  @Override
  public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, String arg1) {
    ObjectMapper mapper = new ObjectMapper();

    var token = tokenRepresentation.getToken();
    try {
      VerifiablePresentationDto tokenVP = mapper.readValue(token, VerifiablePresentationDto.class);
      if (verification.verifyPresentation(tokenVP)) {
        Result<TokenRepresentation> responseToken = obtainClientCredentials(null);
        var claimTokenBuilder = ClaimToken.Builder.newInstance();
        claimTokenBuilder.claim("", responseToken.getContent().getToken());
        return Result.success(claimTokenBuilder.build());
      } else {
        return Result.failure("Invalid Token");
      }
    } catch (JsonProcessingException e) {
      return Result.failure(e.getMessage());
    }
  }
}
