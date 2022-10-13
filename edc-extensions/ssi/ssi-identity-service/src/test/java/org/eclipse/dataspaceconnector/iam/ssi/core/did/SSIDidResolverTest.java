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

package org.eclipse.dataspaceconnector.iam.ssi.core.did;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.iam.ssi.wallet.ManagedIdentityWalletApiServiceImpl;
import org.eclipse.dataspaceconnector.ssi.spi.IdentityWalletApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SSIDidResolverTest {

  private ManagedIdentityWalletApiServiceImpl walletControllerMock =
          mock(ManagedIdentityWalletApiServiceImpl.class);

  private SSIDidResolverImpl didResolver;

  private final String DID_DOCUMENT_FILE = "diddocument.json";

  private String DID_DOCUMENT_STRING;

  @BeforeEach
  public void setUp() {
    didResolver = new SSIDidResolverImpl(mock(IdentityWalletApiService.class));
    try (var stream = getClass().getClassLoader().getResourceAsStream(DID_DOCUMENT_FILE)) {
      DID_DOCUMENT_STRING = new String(stream.readAllBytes());
    } catch (Exception e) {
      fail();
    }
  }


  @Test
  public void resolveDidDocumentTest() throws JsonProcessingException {
    //given
    String did = "1";
    when(walletControllerMock.resolveDid(anyString())).thenReturn(DID_DOCUMENT_STRING);
    DidDocumentDto expectedDidDocument = new ObjectMapper().readValue(DID_DOCUMENT_STRING, DidDocumentDto.class);
    //when
    DidDocumentDto result = didResolver.resolveDid(did);
    //then
    assertThat(expectedDidDocument.getId()).isEqualTo(result.getId());
  }
}
