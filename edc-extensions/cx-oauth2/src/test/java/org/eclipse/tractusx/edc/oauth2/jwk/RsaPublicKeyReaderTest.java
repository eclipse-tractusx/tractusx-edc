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
package org.eclipse.tractusx.edc.oauth2.jwk;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.SneakyThrows;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RsaPublicKeyReaderTest {
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

  private RsaPublicKeyReader publicKeyReader;

  // mocks
  private Monitor monitor;

  @BeforeEach
  void setUp() {
    monitor = Mockito.mock(Monitor.class);
    publicKeyReader = new RsaPublicKeyReader(monitor);
  }

  @Test
  void testCanRead() {
    final JsonWebKey jwk = deserializeKey();

    Assertions.assertTrue(publicKeyReader.canRead(jwk));
  }

  @Test
  void testReadSuccess() {
    final JsonWebKey jwk = deserializeKey();

    final Optional<PublicKeyHolder> key = publicKeyReader.read(jwk);

    Assertions.assertTrue(key.isPresent());
  }

  @SneakyThrows
  private JsonWebKey deserializeKey() {
    final ObjectMapper objectMapper = new ObjectMapper();
    final JsonWebKeySet jwks = objectMapper.readValue(JWKS, JsonWebKeySet.class);
    final JsonWebKey jwk = jwks.getKeys().get(0);

    return jwk;
  }
}
