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

import org.eclipse.dataspaceconnector.iam.ssi.core.SSIIdentityServiceImpl;
import org.eclipse.dataspaceconnector.iam.ssi.model.VerifiablePresentationDto;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class SSIIdentityServiceImplTest {


  SSIIdentityServiceImpl identityService;

  @Test
  public void obtainClientCredentialTest(){
    //given
    String scope = "TestCredentials";
    VerifiablePresentationDto expectedVP = null;
    TokenRepresentation expectedToken = TokenRepresentation.Builder.newInstance().token(expectedVP.toString()).build();
    Result<TokenRepresentation> expectedTokenPresentation = Result.success(expectedToken);
    //when
    Result<TokenRepresentation> result = identityService.obtainClientCredentials(scope);
    //then
    assertEquals(expectedTokenPresentation, result);
  }
}
