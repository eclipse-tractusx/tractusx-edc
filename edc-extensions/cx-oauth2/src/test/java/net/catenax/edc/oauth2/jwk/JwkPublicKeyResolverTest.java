/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package net.catenax.edc.oauth2.jwk;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class JwkPublicKeyResolverTest {

  private static final String JWKS_KEY_ID = "e600c72b-125a-4b30-86a5-9697af62f2a1";
  private static final String JWKS_PUBLIC_KEY =
      "MIICujCCAaKgAwIBAgIECI8fsTANBgkqhkiG9w0BAQsFADAfMR0wGwYDVQQDExR0ZXN0LWFsZXgucmVhY2g1Lm5ldDAeFw0yMDA3MjkwOTM0MjlaFw0yMjAyMTcxNDIwMzNaMB8xHTAbBgNVBAMTFHRlc3QtYWxleC5yZWFjaDUubmV0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlzRszUeQ4WiSqvmYxMP10ngm8ALIoUwMH7Oa8vrZgD5pqalPjetPAxeVcAv2gTyDlOwtB0fGvlQo6n78pd9pTbgrzUjhmFuYN6OCfT6eN/2wu0LmwryFS2mbh7/1DTiKd2tZaRalskPECXTKkeks85HVqanB0860BYlGvQvfgrvhCWXXFJJeXvNwYNFYdDdrFQhoeOAEvRDKg9DdHZf6XzSR6Qk3w51FKn2b7imen/G52itD/kIen1hqqB2Jwt9SWyX5MSGySY2QwC18F6Dfs8L+t0mwCo6grGW9264Z5vlO0PWssEqGIX/ez6nk1ZdHXhoXwJ0W+6QzeQlUN8jNoQIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQAETbMWro4HI4ZuqtnjMZrgEOpx6WhAtxpMx5XFPVWbdp/DpPySotoWbbD6qCtYc34E+ec7mH7aHVap+Gl2IyeSHTht4FXfF9q/1Oj/fis/4DDi1iq00rJsU18D71mZ9FGWCWlO1nhW1KSTGbRJ3E0wSrNabcvaXcwEHokR3zm+xfRWjtbrq2hQ19R16xyOLVy4zrF95QxP4UN+Cvm8nmYur6bSqv+gCMvDsl+O/gtRHGgpUukHEJwnee1R3+1aIv+9zOF3HaaUC5neOLBFITGmeXgi8G2IhbG+JoXh/GUkb66TZUlUAM3qXYNL9Nf+2MQ7nAPTXcxlmImFUUrnv0c3";
  private static final String JWKS =
      String.format(
          "{"
              + "   \"keys\": ["
              + "       {"
              + "           \"kty\": \"RSA\","
              + "           \"e\": \"AQAB\","
              + "           \"x5t\": \"NjU3NDI5ZTZhODU0YjQzMGFiYzkwNGNkZDkwNmZkMzZmOWEzNWVmMQ\","
              + "           \"use\": \"sig\","
              + "           \"kid\": \"%s\","
              + "           \"x5c\": ["
              + "               \"%s\""
              + "           ],"
              + "           \"alg\": \"RS256\","
              + "           \"n\": \"lzRszUeQ4WiSqvmYxMP10ngm8ALIoUwMH7Oa8vrZgD5pqalPjetPAxeVcAv2gTyDlOwtB0fGvlQo6n78pd9pTbgrzUjhmFuYN6OCfT6eN_2wu0LmwryFS2mbh7_1DTiKd2tZaRalskPECXTKkeks85HVqanB0860BYlGvQvfgrvhCWXXFJJeXvNwYNFYdDdrFQhoeOAEvRDKg9DdHZf6XzSR6Qk3w51FKn2b7imen_G52itD_kIen1hqqB2Jwt9SWyX5MSGySY2QwC18F6Dfs8L-t0mwCo6grGW9264Z5vlO0PWssEqGIX_ez6nk1ZdHXhoXwJ0W-6QzeQlUN8jNoQ\""
              + "       }"
              + "   ]"
              + "}",
          JWKS_KEY_ID, JWKS_PUBLIC_KEY);

  private static final URI JWKS_URI = URI.create("https://localhost/.well-known/jwks.json");
  private static final Duration INTERVAL = Duration.ofSeconds(1);

  private JwkPublicKeyResolver jwkPublicKeyResolver;

  // mocks
  private OkHttpClient httpClient;
  private Monitor monitor;
  private PublicKeyReader publicKeyReader;

  @BeforeEach
  void setUp() {
    httpClient = mock(OkHttpClient.class);
    monitor = mock(Monitor.class);
    publicKeyReader = mock(PublicKeyReader.class);

    Mockito.when(publicKeyReader.canRead(any(JsonWebKey.class))).thenReturn(true);
    Mockito.when(publicKeyReader.read(any(JsonWebKey.class)))
        .thenAnswer(
            (i) -> {
              final JsonWebKey jsonWebKey = i.getArgument(0);

              if (jsonWebKey.getKid().equals(JWKS_KEY_ID)) {
                final PublicKeyHolder publicKeyHolder =
                    PublicKeyHolder.builder()
                        .keyId(JWKS_KEY_ID)
                        .publicKey(Mockito.mock(PublicKey.class))
                        .build();
                return Optional.of(publicKeyHolder);
              } else {
                return Optional.empty();
              }
            });
    Mockito.when(httpClient.newCall(any(Request.class)))
        .thenAnswer(
            (i) -> {
              final Response response;

              final Request request = i.getArgument(0);
              if (request.url().toString().equals(JWKS_URI.toString())) {
                final ResponseBody responseBody =
                    ResponseBody.create(JWKS, MediaType.get("application/json"));
                response =
                    new Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_0)
                        .body(responseBody)
                        .message("ok")
                        .code(200)
                        .build();
              } else {
                response = new Response.Builder().code(404).build();
              }

              final Call call = Mockito.mock(Call.class);
              Mockito.when(call.execute()).thenReturn(response);
              return call;
            });

    final TypeManager typeManager = new TypeManager();
    jwkPublicKeyResolver =
        new JwkPublicKeyResolver(
            JWKS_URI,
            httpClient,
            typeManager,
            monitor,
            Collections.singletonList(publicKeyReader),
            INTERVAL);
  }

  @Test
  void testPublicKeyNullForKeyIdNotFound() {
    jwkPublicKeyResolver.start();
    final PublicKey key = jwkPublicKeyResolver.resolveKey("foo");

    Assertions.assertNull(key);
  }

  @Test
  void testPublicKeyFoundById() {
    jwkPublicKeyResolver.start();
    final PublicKey key = jwkPublicKeyResolver.resolveKey(JWKS_KEY_ID);

    Assertions.assertNotNull(key);
  }

  @Test
  void testExceptionOnIdentityProviderRespondingWithNon200() {
    Mockito.when(httpClient.newCall(any(Request.class)))
        .thenAnswer(
            (i) -> {
              final Response response = new Response.Builder().code(404).build();

              final Call call = Mockito.mock(Call.class);
              Mockito.when(call.execute()).thenReturn(response);
              return call;
            });

    Assertions.assertThrows(EdcException.class, () -> jwkPublicKeyResolver.start());
  }

  @Test
  void testExceptionOnIdentityProviderRespondingWithEmptyBody() {
    Mockito.when(httpClient.newCall(any(Request.class)))
        .thenAnswer(
            (i) -> {
              final Response response = new Response.Builder().code(200).build();

              final Call call = Mockito.mock(Call.class);
              Mockito.when(call.execute()).thenReturn(response);
              return call;
            });

    Assertions.assertThrows(EdcException.class, () -> jwkPublicKeyResolver.start());
  }

  @Test
  void testExceptionOnIdentityProviderRespondingWithEmptyJwks() {
    Mockito.when(httpClient.newCall(any(Request.class)))
        .thenAnswer(
            (i) -> {
              final ResponseBody responseBody =
                  ResponseBody.create("{ \"keys\": [] }", MediaType.get("application/json"));
              final Response response = new Response.Builder().code(200).body(responseBody).build();

              final Call call = Mockito.mock(Call.class);
              Mockito.when(call.execute()).thenReturn(response);
              return call;
            });

    Assertions.assertThrows(EdcException.class, () -> jwkPublicKeyResolver.start());
  }

  @Test
  void testExceptionOnHttpClientException() {
    Mockito.when(httpClient.newCall(any(Request.class))).thenThrow(new RuntimeException());

    Assertions.assertThrows(EdcException.class, () -> jwkPublicKeyResolver.start());
  }
}
