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
package org.eclipse.tractusx.edc.ssi.core.identity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.tractusx.edc.ssi.core.SSIIdentityServiceImpl;
import org.eclipse.tractusx.edc.ssi.core.claims.SSIClaims;
import org.eclipse.tractusx.edc.ssi.core.claims.SSIVerifiableCredentialsImpl;
import org.eclipse.tractusx.edc.ssi.core.claims.SSIVerifiablePresentationImpl;
import org.eclipse.tractusx.edc.ssi.miw.model.VerifiablePresentationDto;
import org.eclipse.tractusx.edc.ssi.miw.wallet.ManagedIdentityWalletApiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SSIIdentityServiceExtensionTest {

  private final String VP_FILE = "verifiablepresentation.json";
  private VerifiablePresentationDto vpDto = null;
  private SSIIdentityServiceImpl identityService;

  private ManagedIdentityWalletApiServiceImpl mockWallet;
  private SSIVerifiableCredentialsImpl vcImpl;
  private SSIVerifiablePresentationImpl vpImpl;
  private SSIClaims ssiClaims;

  @BeforeEach
  public void setUp() throws IOException {

    ObjectMapper mapper = new ObjectMapper();
    var vcStream = getClass().getClassLoader().getResourceAsStream(VP_FILE);
    String vcString = new String(vcStream.readAllBytes());
    this.vpDto = mapper.readValue(vcString, VerifiablePresentationDto.class);

    mockWallet = mock(ManagedIdentityWalletApiServiceImpl.class);
    vcImpl = mock(SSIVerifiableCredentialsImpl.class);
    vpImpl = mock(SSIVerifiablePresentationImpl.class);
    ssiClaims = mock(SSIClaims.class);
    identityService = new SSIIdentityServiceImpl(mockWallet, vcImpl, vpImpl);
  }

  @Test
  void testObtainClientCredentials() throws JsonProcessingException {
    when(ssiClaims.getVerifiablePresentation(anyString())).thenReturn(vpDto);
    when(ssiClaims.makeTokenFromVerifiablePresentation(vpDto))
        .thenReturn(TokenRepresentation.Builder.newInstance().token(vpDto.toString()).build());
    assertDoesNotThrow(() -> identityService.obtainClientCredentials(null));
  }
}
