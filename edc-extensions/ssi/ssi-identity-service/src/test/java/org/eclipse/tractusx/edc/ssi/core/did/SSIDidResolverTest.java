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

package org.eclipse.tractusx.edc.ssi.core.did;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.edc.ssi.miw.wallet.ManagedIdentityWalletApiServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SSIDidResolverTest {

  private ManagedIdentityWalletApiServiceImpl walletControllerMock =
      mock(ManagedIdentityWalletApiServiceImpl.class);

  private SSIDidResolverImpl didResolver;

  private final String DID_DOCUMENT_FILE = "diddocument.json";

  private String DID_DOCUMENT_STRING;

  @BeforeEach
  public void setUp() {
    didResolver = new SSIDidResolverImpl(walletControllerMock);
    try (var stream = getClass().getClassLoader().getResourceAsStream(DID_DOCUMENT_FILE)) {
      DID_DOCUMENT_STRING = new String(stream.readAllBytes());
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void resolveDidDocumentTest() throws JsonProcessingException {
    // given
    String did = "1";
    when(walletControllerMock.resolveDid(anyString())).thenReturn(DID_DOCUMENT_STRING);
    DidDocumentDto expectedDidDocument =
        new ObjectMapper().readValue(DID_DOCUMENT_STRING, DidDocumentDto.class);
    // when
    DidDocumentDto result = didResolver.resolveDid(did);
    // then
    Assertions.assertEquals(expectedDidDocument.getId(), result.getId());
  }
}
