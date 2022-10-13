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
package org.eclipse.dataspaceconnector.iam.ssi.wallet;


import okhttp3.*;
import org.eclipse.dataspaceconnector.iam.ssi.model.VerifiableCredentialRegistry;
import org.eclipse.dataspaceconnector.iam.ssi.model.AccessTokenDescriptionDto;
import org.eclipse.dataspaceconnector.iam.ssi.model.AccessTokenRequestDto;
import org.eclipse.dataspaceconnector.iam.ssi.model.VerifiableCredentialDto;
import org.eclipse.dataspaceconnector.iam.ssi.model.WalletDescriptionDto;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import jakarta.ws.rs.InternalServerErrorException;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.ssi.spi.IdentityWalletApiService;

import static java.lang.String.format;

public class ManagedIdentityWalletApiServiceImpl implements IdentityWalletApiService {

  private final Monitor monitor;
  private final String logPrefix;
  private final ManagedIdentityWalletConfig config;
  private final OkHttpClient httpClient;

  private final ObjectMapper objectMapper;

  private final AccessTokenRequestDto accessTokenRequestDto;

  private final VerifiableCredentialRegistry credentialRegistry;

  public ManagedIdentityWalletApiServiceImpl(Monitor monitor,
                                             String logPrefix,
                                             ManagedIdentityWalletConfig config,
                                             OkHttpClient httpClient,
                                             TypeManager typeManager,
                                             VerifiableCredentialRegistry credentialRegistry) {
    this.monitor = monitor;
    this.logPrefix = logPrefix;
    this.config = config;
    this.httpClient = httpClient;
    this.objectMapper = typeManager.getMapper();
    this.credentialRegistry = credentialRegistry;
    AccessTokenRequestDto.Builder builder = AccessTokenRequestDto.Builder.newInstance();
    this.accessTokenRequestDto = builder.clientID(config.getKeycloakClientID())
            .clientSecret(config.getKeycloakClientSecret())
            .grandType(config.getKeycloakGrandType())
            .scope(config.getKeycloakScope())
            .build();
    monitor.info(format("%s :: Initialized Wallet with values: " + config, logPrefix));
  }

  public String issueVerifiablePresentation(String verifiableCredentialJson) {
    monitor.info(format("%s :: Received a presentation request for presentation", logPrefix));
    var url = config.getWalletURL() + "/api/presentations";
    try {
      AccessTokenDescriptionDto accessToken = getKeyCloakToken(this.accessTokenRequestDto);
      RequestBody reqBody = RequestBody.create(
              MediaType.parse("application/json"), verifiableCredentialJson);
      Request request = new Request.Builder()
              .url(url)
              .header("Authorization", "Bearer " + accessToken.getAccessToken())
              .post(reqBody)
              .build();
      var response = httpClient.newCall(request).execute();
      var body = response.body();
      if (!response.isSuccessful() || body == null) {
        throw new InternalServerErrorException(format("MIW responded with: %s %s", response.code(), body != null ? body.string() : ""));
      }
      monitor.info("Fetched VP");
      return body.string();
    } catch (Exception ex) {
      throw new EdcException(ex.getMessage());
    }
  }

  public String resolveDid(String did) {
    monitor.info(format("%s :: Received a did request for did " + did, logPrefix));
    var url = config.getWalletURL() + "/api/didDocuments/" + did;
    AccessTokenDescriptionDto accessToken = null;

    try {
      accessToken = getKeyCloakToken(this.accessTokenRequestDto);
      monitor.severe(format("Fetched AccessToken %s", accessToken));
      Request request = new Request.Builder()
              .url(url)
              .header("Authorization", "Bearer " + accessToken.getAccessToken())
              .build();
      var response = httpClient.newCall(request).execute();
      var body = response.body();
      if (!response.isSuccessful() || body == null) {
        throw new InternalServerErrorException(format("Keycloak responded with: %s %s", response.code(), body != null ? body.string() : ""));
      }
      monitor.info("Fetched Did");
      return body.string();
    } catch (Exception e) {
      monitor.severe(format("Error by fetching Did at %s", url), e);
      throw new EdcException(e.getMessage());
    }
  }

  public String validateVerifablePresentation(String verifiablePresentationJson) {
    monitor.info(format("%s :: Received a VP validation reques", logPrefix));
    var url = config.getWalletURL() + "/api/presentations/validation?withDateValidation=false";
    try {
      AccessTokenDescriptionDto accessToken = getKeyCloakToken(this.accessTokenRequestDto);
      RequestBody reqBody = RequestBody.create(
              MediaType.parse("application/json"), verifiablePresentationJson);
      Request request = new Request.Builder()
              .url(url)
              .header("Authorization", "Bearer " + accessToken.getAccessToken())
              .post(reqBody)
              .build();
      var response = httpClient.newCall(request).execute();
      var body = response.body();
      if (!response.isSuccessful() || body == null) {
        monitor.info("VP invalid");
        throw new InternalServerErrorException(format("MIW responded with: %s %s", response.code(), body != null ? body.string() : ""));
      }
      monitor.info("VP valid");
      return body.string();
    } catch (Exception ex) {
      throw new EdcException(ex.getMessage());
    }
  }


  public void fetchWalletDescription() {
    monitor.info(format("%s :: Start fetching Wallet Data", logPrefix));
    try {
      var url = config.getWalletURL() + "/api/wallets/" + config.getWalletDID() + "?withCredentials=true";
      AccessTokenDescriptionDto accessToken = getKeyCloakToken(this.accessTokenRequestDto);
      Request request = new Request.Builder()
              .url(url)
              .header("Authorization", "Bearer " + accessToken.getAccessToken())
              .build();
      var response = httpClient.newCall(request).execute();
      var body = response.body();
      if (!response.isSuccessful() || body == null) {
        throw new InternalServerErrorException(format("MIW responded with: %s %s", response.code(), body != null ? body.string() : ""));
      }
      WalletDescriptionDto walletDescriptionDto = objectMapper.readValue(body.string(), WalletDescriptionDto.class);
      fillRegistry(walletDescriptionDto.getVerifiableCredentials());
      monitor.info("Saved Wallet data in registry");
    } catch (Exception e) {
      monitor.severe(format("Error in fetching AccessToken"), e);
    }
  }

  @Override
  public String getOwnerBPN() {
    return config.getOwnerBPN();
  }


  private void fillRegistry(List<VerifiableCredentialDto> credentialDtoList) {
    credentialRegistry.clearRegistry();
    for (VerifiableCredentialDto vc : credentialDtoList) {
      credentialRegistry.addVerifableCredential(vc);
    }
  }

  private AccessTokenDescriptionDto getKeyCloakToken(AccessTokenRequestDto accessTokenRequest) throws IOException {
    var url = config.getAccessTokenURL();
    RequestBody formBody = new FormBody.Builder()
            .add("grant_type", accessTokenRequest.getGrantType())
            .add("client_id", accessTokenRequest.getCliendId())
            .add("client_secret", accessTokenRequest.getClient_secret())
            .add("scope", accessTokenRequest.getScope()).build();
    var request = new Request.Builder()
            .url(url)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(formBody);

    try (var response = httpClient.newCall(request.build()).execute()) {
      var body = response.body();
      if (!response.isSuccessful() || body == null) {
        throw new InternalServerErrorException(format("Keycloak responded with: %s %s", response.code(), body != null ? body.string() : ""));
      }
      var accessTokenDescription = objectMapper.readValue(body.string(), AccessTokenDescriptionDto.class);
      monitor.info("Get new token with ID: " + accessTokenDescription.getTokenID());
      return accessTokenDescription;
    } catch (Exception e) {
      monitor.severe(format("Error in calling the keycloak server at %s", url), e);
      throw e;
    }
  }
}