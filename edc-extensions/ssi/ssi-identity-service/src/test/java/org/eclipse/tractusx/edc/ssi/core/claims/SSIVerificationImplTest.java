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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.eclipse.tractusx.edc.ssi.core.did.DidDocumentDto;
import org.eclipse.tractusx.edc.ssi.miw.model.VerifiablePresentationDto;
import org.eclipse.tractusx.edc.ssi.miw.wallet.ManagedIdentityWalletApiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SSIVerificationImplTest {

  private final String DID_DOCUMENT_FILE = "diddocument.json";
  private final String VERIFIABLE_PRESENTATION_FILE = "verifiablepresentation.json";

  DidDocumentDto didDocumentDto = null;
  VerifiablePresentationDto verifiablePresentationDto = null;

  private SSIVerificationImpl verification;
  private ManagedIdentityWalletApiServiceImpl mockWallet;

  @BeforeEach
  public void setUp() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    var vpDocStream = getClass().getClassLoader().getResourceAsStream(VERIFIABLE_PRESENTATION_FILE);
    String vpDoc = new String(vpDocStream.readAllBytes());
    this.verifiablePresentationDto = mapper.readValue(vpDoc, VerifiablePresentationDto.class);
    mockWallet = mock(ManagedIdentityWalletApiServiceImpl.class);
    verification = new SSIVerificationImpl(mockWallet);
  }

  @Test
  public void testVerifyPresentation_Success() {
    // given
    when(mockWallet.validateVerifiablePresentation(anyString())).thenReturn(true);
    // when
    boolean result = verification.verifyPresentation(verifiablePresentationDto);
    // then
    assertEquals(true, result);
  }
}
