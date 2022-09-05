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
 *
 */

package net.catenax.edc.oauth2.jwt.validation;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.PublicKey;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.PublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.jwt.TokenValidationRulesRegistry;
import org.eclipse.dataspaceconnector.spi.jwt.TokenValidationService;
import org.eclipse.dataspaceconnector.spi.result.Result;

@RequiredArgsConstructor
public class TokenValidationServiceImpl implements TokenValidationService {

  @NonNull private final PublicKeyResolver publicKeyResolver;

  @NonNull private final TokenValidationRulesRegistry rulesRegistry;

  @Override
  public Result<ClaimToken> validate(@NonNull final TokenRepresentation tokenRepresentation) {
    final String token = tokenRepresentation.getToken();
    final Map<String, Object> additional = tokenRepresentation.getAdditional();
    final JWTClaimsSet claimsSet;
    try {
      final SignedJWT signedJwt = SignedJWT.parse(token);
      final String publicKeyId = signedJwt.getHeader().getKeyID();
      final Result<JWSVerifier> verifierCreationResult =
          createVerifier(signedJwt.getHeader(), publicKeyId);

      if (verifierCreationResult.failed()) {
        return Result.failure(verifierCreationResult.getFailureMessages());
      }

      if (!signedJwt.verify(verifierCreationResult.getContent())) {
        return Result.failure("Token verification failed");
      }

      claimsSet = signedJwt.getJWTClaimsSet();

      final List<String> errors =
          rulesRegistry.getRules().stream()
              .map(r -> r.checkRule(signedJwt, additional))
              .filter(Result::failed)
              .map(Result::getFailureMessages)
              .flatMap(Collection::stream)
              .collect(Collectors.toList());

      if (!errors.isEmpty()) {
        return Result.failure(errors);
      }

      final ClaimToken.Builder tokenBuilder = ClaimToken.Builder.newInstance();

      claimsSet.getClaims().entrySet().stream()
          .map(entry -> Map.entry(entry.getKey(), Objects.toString(entry.getValue())))
          .filter(entry -> entry.getValue() != null)
          .forEach(entry -> tokenBuilder.claim(entry.getKey(), entry.getValue()));

      return Result.success(tokenBuilder.build());

    } catch (final JOSEException e) {
      return Result.failure(e.getMessage());
    } catch (final ParseException e) {
      return Result.failure("Failed to decode token");
    }
  }

  private Result<JWSVerifier> createVerifier(final JWSHeader header, final String publicKeyId) {
    final PublicKey publicKey = publicKeyResolver.resolveKey(publicKeyId);
    if (publicKey == null) {
      return Result.failure("Failed to resolve public key with id: " + publicKeyId);
    }
    try {
      return Result.success(new DefaultJWSVerifierFactory().createJWSVerifier(header, publicKey));
    } catch (final JOSEException e) {
      return Result.failure("Failed to create verifier");
    }
  }
}
