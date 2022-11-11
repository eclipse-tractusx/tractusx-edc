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

package org.eclipse.tractusx.edc.ssi.core.claims;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ipfs.multibase.Base58;
import java.io.IOException;
import org.eclipse.tractusx.edc.ssi.core.did.DidDocumentDto;
import org.eclipse.tractusx.edc.ssi.core.did.DidVerificationMethodDto;
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
    var didDocStream = getClass().getClassLoader().getResourceAsStream(DID_DOCUMENT_FILE);
    String didDoc = new String(didDocStream.readAllBytes());
    var vpDocStream = getClass().getClassLoader().getResourceAsStream(VERIFIABLE_PRESENTATION_FILE);
    String vpDoc = new String(vpDocStream.readAllBytes());

    this.didDocumentDto = mapper.readValue(didDoc, DidDocumentDto.class);
    this.verifiablePresentationDto = mapper.readValue(vpDoc, VerifiablePresentationDto.class);
    mockWallet = mock(ManagedIdentityWalletApiServiceImpl.class);
    verification = new SSIVerificationImpl(mockWallet);

    when(mockWallet.resolveDid(anyString())).thenReturn(didDoc);
  }

  @Test
  public void testVerifyPresentation_Success() throws IOException {
    // given
    DidVerificationMethodDto didVerifyMethod = didDocumentDto.getVerificationMethodDtos().get(0);
    byte[] publicKey = Base58.decode(didVerifyMethod.getPublicKeyBase58());
    // when
    // Boolean result = verification.verifyPresentation(verifiablePresentationDto);
    // then
    // assertEquals(result, true);
  }
}
