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
package net.catenax.edc.oauth2.jwt.validation;

import static java.time.ZoneOffset.UTC;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.jwt.TokenValidationRule;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class NbfValidationRule implements TokenValidationRule {
  @NonNull private final Duration notBeforeValidationLeeway;

  @NonNull private final Clock clock;

  /**
   * Validates the JWT by checking the audience, nbf, and expiration. Accessible for testing.
   *
   * @param toVerify The jwt including the claims.
   * @param additional No more additional information needed for this validation, can be null.
   */
  @Override
  public Result<SignedJWT> checkRule(
      final SignedJWT toVerify, @Nullable final Map<String, Object> additional) {
    try {
      final JWTClaimsSet claimsSet = toVerify.getJWTClaimsSet();
      final List<String> errors = new ArrayList<>();

      final Instant now = clock.instant();
      final Instant leewayNow = now.plusSeconds(notBeforeValidationLeeway.toSeconds());
      final Date notBefore = claimsSet.getNotBeforeTime();
      if (notBefore == null) {
        errors.add("Required not before (nbf) claim is missing in token");
      } else if (leewayNow.isBefore(dateToInstant(notBefore))) {
        errors.add("Current date/time with leeway before the not before (nbf) claim in token");
      }

      if (errors.isEmpty()) {
        return Result.success(toVerify);
      } else {
        return Result.failure(errors);
      }
    } catch (final ParseException parseException) {
      throw new EdcException(
          String.format(
              "%s: unable to parse SignedJWT (%s)",
              this.getClass().getSimpleName(), parseException.getMessage()));
    }
  }

  private static Instant dateToInstant(final Date date) {
    return ZonedDateTime.ofInstant(date.toInstant(), UTC).toInstant();
  }
}
