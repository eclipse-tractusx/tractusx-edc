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
package org.eclipse.tractusx.edc.ssi.miw.wallet;

import static java.lang.String.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.InternalServerErrorException;
import java.io.IOException;
import java.util.List;
import okhttp3.*;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.tractusx.edc.ssi.miw.model.*;
import org.eclipse.tractusx.edc.ssi.miw.registry.VerifiableCredentialRegistry;
import org.eclipse.tractusx.edc.ssi.spi.IdentityWalletApiService;

public class ManagedIdentityWalletApiServiceImpl implements IdentityWalletApiService {

  public static final MediaType JSON_MEDIA_TYPE =
      MediaType.parse("application/json; charset=utf-8");

  public static final String HEADER_AUTHORIZATION_KEY = "Authorization";

  public static final String VALIDATE_PRESENTATION_URL =
      "%s/api/presentations/validation?withDateValidation=false&withRevocationValidation=true";

  public static final String RESOLVE_DID_URL = "%s/api/didDocuments/%s";

  public static final String PRESENTATIONS_URL = "%s/api/presentations";

  public static final String FETCH_WALLET_URL = "%s/api/wallets/%s?withCredentials=true";

  private final Monitor monitor;
  private final String logPrefix;
  private final ManagedIdentityWalletConfig config;
  private final OkHttpClient httpClient;

  private final ObjectMapper objectMapper;

  private final AccessTokenRequestDto accessTokenRequestDto;

  private final VerifiableCredentialRegistry credentialRegistry;

  public ManagedIdentityWalletApiServiceImpl(
      Monitor monitor,
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
    this.accessTokenRequestDto =
        builder
            .clientID(config.getKeycloakClientID())
            .clientSecret(config.getKeycloakClientSecret())
            .grandType(config.getKeycloakGrandType())
            .scope(config.getKeycloakScope())
            .build();
    monitor.info(format("%s :: Initialized Wallet with values: %s", this.logPrefix, config));
  }

  public String issueVerifiablePresentation(String verifiableCredentialJson) {
    monitor.info(format("%s :: Received a presentation request for presentation", this.logPrefix));
    String url = format(PRESENTATIONS_URL, config.getWalletURL());
    try {
      RequestBody reqBody = RequestBody.create(verifiableCredentialJson, JSON_MEDIA_TYPE);
      Request request =
          new Request.Builder()
              .url(url)
              .header(HEADER_AUTHORIZATION_KEY, getAccessToken(this.accessTokenRequestDto))
              .post(reqBody)
              .build();
      var response = httpClient.newCall(request).execute();
      checkMIWResponse(response);
      monitor.info("Fetched VP");
      return response.body().string();
    } catch (Exception ex) {
      throw new EdcException(ex.getMessage());
    }
  }

  public String resolveDid(String did) {
    monitor.info(format("%s :: Received a did request for did " + did, this.logPrefix));
    String url = format(RESOLVE_DID_URL, config.getWalletURL(), did);
    try {
      Request request =
          new Request.Builder()
              .url(url)
              .header(HEADER_AUTHORIZATION_KEY, getAccessToken(this.accessTokenRequestDto))
              .build();
      var response = httpClient.newCall(request).execute();
      checkMIWResponse(response);
      monitor.info("Fetched Did");
      return response.body().string();
    } catch (Exception e) {
      monitor.severe(format("Error by fetching Did at %s", url), e);
      throw new EdcException(e.getMessage());
    }
  }

  public boolean validateVerifiablePresentation(String verifiablePresentationJson) {
    monitor.info(format("%s :: Received a VP validation request", this.logPrefix));
    String url = format(VALIDATE_PRESENTATION_URL, config.getWalletURL());
    try {
      // check if the verifiable credential is issued by trusted provider
      VerifiablePresentationDto vp =
          objectMapper.readValue(verifiablePresentationJson, VerifiablePresentationDto.class);
      checkIfCredentialsAreSignedByTrustedIssuers(vp);
      RequestBody reqBody = RequestBody.create(verifiablePresentationJson, JSON_MEDIA_TYPE);
      Request request =
          new Request.Builder()
              .url(url)
              .header(HEADER_AUTHORIZATION_KEY, getAccessToken(this.accessTokenRequestDto))
              .post(reqBody)
              .build();
      var response = httpClient.newCall(request).execute();
      String error = "";
      if (response.isSuccessful() && response.body() != null) {
        VerifyResponse verifyResponse =
            objectMapper.readValue(response.body().string(), VerifyResponse.class);
        if (verifyResponse.isValid()) {
          monitor.info(format("VP %s is valid", vp.getId()));
          return true;
        } else if (verifyResponse.getError() != null) {
          error = verifyResponse.getError();
        }
      }
      monitor.info("VP invalid");
      throw new InternalServerErrorException(
          format("MIW responded with: %s %s", response.code(), error));
    } catch (Exception ex) {
      throw new EdcException(ex.getMessage());
    }
  }

  public void fetchWalletDescription() {
    monitor.info(format("%s :: Start fetching Wallet Data", this.logPrefix));
    String url = format(FETCH_WALLET_URL, config.getWalletURL(), config.getWalletDID());
    try {
      Request request =
          new Request.Builder()
              .url(url)
              .header(HEADER_AUTHORIZATION_KEY, getAccessToken(this.accessTokenRequestDto))
              .build();
      var response = httpClient.newCall(request).execute();
      checkMIWResponse(response);
      WalletDescriptionDto walletDescriptionDto =
          objectMapper.readValue(response.body().string(), WalletDescriptionDto.class);
      fillRegistry(walletDescriptionDto.getVerifiableCredentials());
      monitor.info("Saved Wallet data in registry");
    } catch (Exception e) {
      monitor.severe("Error in fetching Wallet", e);
    }
  }

  @Override
  public String getOwnerBPN() {
    return config.getOwnerBPN();
  }

  private void fillRegistry(List<VerifiableCredentialDto> credentialDtoList) {
    credentialRegistry.clearRegistry();
    for (VerifiableCredentialDto vc : credentialDtoList) {
      credentialRegistry.addVerifiableCredential(vc);
    }
  }

  private String getAccessToken(AccessTokenRequestDto accessTokenRequest) throws IOException {
    String url = config.getAccessTokenURL();
    RequestBody formBody =
        new FormBody.Builder()
            .add("grant_type", accessTokenRequest.getGrantType())
            .add("client_id", accessTokenRequest.getClientId())
            .add("client_secret", accessTokenRequest.getClient_secret())
            .add("scope", accessTokenRequest.getScope())
            .build();
    var request =
        new Request.Builder()
            .url(url)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(formBody);

    try (var response = httpClient.newCall(request.build()).execute()) {
      var body = response.body();
      if (!response.isSuccessful() || body == null) {
        throw new InternalServerErrorException(
            format(
                "Keycloak responded with: %s %s",
                response.code(), body != null ? body.string() : ""));
      }
      var accessTokenDescription =
          objectMapper.readValue(body.string(), AccessTokenDescriptionDto.class);
      monitor.info("Get new token with ID: " + accessTokenDescription.getTokenID());
      return "Bearer " + accessTokenDescription.getAccessToken();
    } catch (Exception e) {
      monitor.severe(format("Error in calling the keycloak server at %s", url), e);
      throw e;
    }
  }

  private void checkMIWResponse(Response response) throws IOException {
    var body = response.body();
    if (!response.isSuccessful() || body == null) {
      throw new InternalServerErrorException(
          format("MIW responded with: %s %s", response.code(), body != null ? body.string() : ""));
    }
  }

  private void checkIfCredentialsAreSignedByTrustedIssuers(VerifiablePresentationDto vp) {
    vp.getVerifiableCredentials()
        .forEach(
            vc -> {
              if (!config.getDidOfTrustedProviders().contains(vc.getIssuer())) {
                monitor.info(
                    format(
                        "VC with Id %s issued by an untrusted provider %s",
                        vc.getId(), vc.getIssuer()));
                throw new InternalServerErrorException(
                    "The Verifiable presentation includes VCs which are issued by an"
                        + "untrusted provider");
              } else {
                monitor.info(format("VC %s signed by a trusted provider!", vc.getId()));
              }
            });
  }
}
