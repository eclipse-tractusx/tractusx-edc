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

package org.eclipse.tractusx.edc.ssi.miw.wallet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Properties;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.tractusx.edc.ssi.miw.model.AccessTokenRequestDto;
import org.eclipse.tractusx.edc.ssi.miw.registry.VerifiableCredentialRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ManagedIdentityWalletApiServiceImplTest {

  private ManagedIdentityWalletConfig walletConfigMock = mock(ManagedIdentityWalletConfig.class);
  private ManagedIdentityWalletApiServiceImpl walletApiService;
  Properties configProps;
  private MockWebServer mockWebServer;

  private final String WALLET_DESCRIPTION_JSON = "walletdescription.json";
  private final String DID_DOCUMENT_JSON = "diddocument.json";
  private final String ACCESSTOKEN_GRANDTYPE_PROP = "ssi.miw.keycloak.grandtype";
  private final String ACCESSTOKEN_URL_PROP = "ssi.miw.accesstoken.url";
  private final String ACCESSTOKEN_CLIENTID_PROP = "ssi.miw.keycloak.clientid";
  private final String ACCESSTOKEN_CLIENTSECRET_PROP = "ssi.miw.keycloak.clientsecret";
  private final String ACCESSTOKEN_SCOPE_PROP = "ssi.miw.keycloak.scope";

  private String VALID_WALLET_DESCRIPTION_RESPONSE;
  private String VALID_DID_DOCUMENT_RESPONSE;

  @BeforeEach
  public void setUp() throws IOException {
    var stream = getClass().getClassLoader().getResourceAsStream("walletConfig.properties");
    configProps = new Properties();
    configProps.load(stream);

    VALID_WALLET_DESCRIPTION_RESPONSE =
        new String(
            getClass()
                .getClassLoader()
                .getResourceAsStream(WALLET_DESCRIPTION_JSON)
                .readAllBytes());

    VALID_DID_DOCUMENT_RESPONSE =
        new String(
            getClass().getClassLoader().getResourceAsStream(DID_DOCUMENT_JSON).readAllBytes());

    this.mockWebServer = new MockWebServer();
    this.walletApiService =
        new ManagedIdentityWalletApiServiceImpl(
            mock(Monitor.class),
            "testService",
            walletConfigMock,
            mock(OkHttpClient.class),
            mock(TypeManager.class),
            mock(VerifiableCredentialRegistry.class));
  }

  @Test
  public void verifyGetKeyCloakTokenSuccess() throws IOException {
    // given
    // TODO mock http client response and check
    mockWebServer.enqueue(
        new MockResponse()
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .setResponseCode(500));
    AccessTokenRequestDto tokenMock = mock(AccessTokenRequestDto.class);
    when(walletConfigMock.getAccessTokenURL())
        .thenReturn(configProps.getProperty(ACCESSTOKEN_URL_PROP));
    when(tokenMock.getGrantType()).thenReturn(configProps.getProperty(ACCESSTOKEN_GRANDTYPE_PROP));
    when(tokenMock.getClientId()).thenReturn(configProps.getProperty(ACCESSTOKEN_CLIENTID_PROP));
    when(tokenMock.getClient_secret())
        .thenReturn(configProps.getProperty(ACCESSTOKEN_CLIENTSECRET_PROP));
    when(tokenMock.getScope()).thenReturn(configProps.getProperty(ACCESSTOKEN_SCOPE_PROP));
    // when
    // AccessTokenDescriptionDto result = walletApiService.getKeyCloakToken(tokenMock);
    // then
    // assertNotNull(result);
  }
}
