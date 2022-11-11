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

package org.eclipse.tractusx.edc.ssi.core.claims;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.tractusx.edc.ssi.core.utils.VerifiableCredentialException;
import org.eclipse.tractusx.edc.ssi.miw.model.VerifiableCredentialDto;
import org.eclipse.tractusx.edc.ssi.miw.model.VerifiablePresentationDto;

/**
 * SSI Claims finds the needed Verifiable Credential with a given Scope and transforms them into
 * Verifiable Presentations and builds a Tokenpresentation out of them
 */
public class SSIClaims {

  SSIVerifiableCredentials verifiableCredentials;
  SSIVerifiablePresentation verifiablePresentation;

  public SSIClaims(
      SSIVerifiableCredentials verifiableCredentials,
      SSIVerifiablePresentation verifiablePresentation) {
    this.verifiableCredentials = verifiableCredentials;
    this.verifiablePresentation = verifiablePresentation;
  }

  public VerifiablePresentationDto getVerifiablePresentation(String scope) {
    // Try to get Credentials with Scope
    try {
      VerifiableCredentialDto vc = verifiableCredentials.findByScope(scope);
      return verifiablePresentation.getPresentation(vc);
    } catch (Exception e) {
      throw new VerifiableCredentialException(e.getMessage());
    }
  }

  public TokenRepresentation makeTokenFromVerifiablePresentation(VerifiablePresentationDto vp)
      throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    String tokenVP = mapper.writeValueAsString(vp);
    TokenRepresentation token = TokenRepresentation.Builder.newInstance().token(tokenVP).build();
    return token;
  }
}
