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
import org.eclipse.dataspaceconnector.iam.ssi.core.utils.VerifiableCredentialException;
import org.eclipse.dataspaceconnector.iam.ssi.model.VerifiableCredentialDto;
import org.eclipse.dataspaceconnector.iam.ssi.model.VerifiablePresentationDto;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;

/**
 * SSI Claims finds the needed Verifiable Credential with a given Scope and transforms them into
 * Verifiable Presentations and builds a Tokenpresentation out of them
 */
public class SSIClaims {

  SSIVerifiableCredentials verifiableCredentials;
  SSIVerifiablePresentation verifiablePresentation;

  public SSIClaims(SSIVerifiableCredentials verifiableCredentials, SSIVerifiablePresentation verifiablePresentation) {
    this.verifiableCredentials = verifiableCredentials;
    this.verifiablePresentation = verifiablePresentation;
  }

  public VerifiablePresentationDto getVerifiablePresentation(String scope) throws Exception{
    //Try to get Credentials with Scope
    try{
      VerifiableCredentialDto vc = verifiableCredentials.findByScope(scope);
      VerifiablePresentationDto vp = verifiablePresentation.getPresentation(vc);

      return vp;
    } catch (Exception e){
      throw new VerifiableCredentialException(e.getMessage());
    }
  }

  public TokenRepresentation makeTokenFromVerifiablePresentation(VerifiablePresentationDto vp) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    String tokenVP = mapper.writeValueAsString(vp);
    TokenRepresentation token = TokenRepresentation.Builder.newInstance().token(tokenVP).build();
    return token;
  }
}
