/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *       Microsoft Corporation - Simplified token representation
 *       Mercedes Benz Tech Innovation - Rename class, add Type-Safety
 *
 */

package org.eclipse.tractusx.edc.oauth2.jwt.generator;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.util.Map.Entry;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.jwt.JwtDecorator;
import org.eclipse.dataspaceconnector.spi.jwt.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

public class JwtTokenGenerationService implements TokenGenerationService {
  private static final String KEY_ALGO_RSA = "RSA";
  private static final String KEY_ALGO_EC = "EC";

  private final JWSAlgorithm jwsAlgorithm;
  private final JWSSigner jwsSigner;

  public JwtTokenGenerationService(@NonNull final PrivateKey privateKey) {
    this.jwsAlgorithm = getJWSAlgorithm(privateKey.getAlgorithm());
    this.jwsSigner = getJWSSigner(privateKey.getAlgorithm(), privateKey);
  }

  @SneakyThrows
  private static JWSSigner getJWSSigner(
      @NonNull final String algorithm, @NonNull final PrivateKey privateKey) {
    if (algorithm.equals(KEY_ALGO_EC)) {
      return new ECDSASigner((ECPrivateKey) privateKey);
    }

    if (algorithm.equals(KEY_ALGO_RSA)) {
      return new RSASSASigner(privateKey);
    }

    throw new EdcException("Unsupported key algorithm: " + algorithm);
  }

  private static JWSAlgorithm getJWSAlgorithm(@NonNull final String algorithm) {
    if (algorithm.equals(KEY_ALGO_EC)) {
      return JWSAlgorithm.ES256;
    }

    if (algorithm.equals(KEY_ALGO_RSA)) {
      return JWSAlgorithm.RS256;
    }

    throw new EdcException("Unsupported key algorithm: " + algorithm);
  }

  @Override
  public Result<TokenRepresentation> generate(@NotNull @NonNull final JwtDecorator... decorators) {

    final JWSHeader.Builder headerBuilder = new JWSHeader.Builder(jwsAlgorithm);
    final JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder();

    for (JwtDecorator decorator : decorators) {
      for (Entry<String, Object> claim : decorator.claims().entrySet()) {
        claimsBuilder.claim(claim.getKey(), claim.getValue());
      }
      headerBuilder.customParams(decorator.headers());
    }

    final JWTClaimsSet jwtClaimSet = claimsBuilder.build();

    final SignedJWT signedJwt = new SignedJWT(headerBuilder.build(), jwtClaimSet);
    try {
      signedJwt.sign(jwsSigner);
    } catch (final JOSEException joseException) {
      return Result.failure("Failed to sign token");
    }

    return Result.success(
        TokenRepresentation.Builder.newInstance().token(signedJwt.serialize()).build());
  }
}
