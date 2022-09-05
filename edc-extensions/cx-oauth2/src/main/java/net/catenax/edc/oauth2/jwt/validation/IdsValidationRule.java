/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - Initial Implementation
 *
 */

package net.catenax.edc.oauth2.jwt.validation;

import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.util.Map;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.jwt.TokenValidationRule;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.Nullable;

public class IdsValidationRule implements TokenValidationRule {
  private final boolean validateReferring;

  public IdsValidationRule(boolean validateReferring) {
    this.validateReferring = validateReferring;
  }

  /** Validates the JWT by checking extended IDS rules. */
  @Override
  public Result<SignedJWT> checkRule(SignedJWT jwt, @Nullable Map<String, Object> additional) {
    if (additional != null) {
      var issuerConnector = additional.get("issuerConnector");
      if (issuerConnector == null) {
        return Result.failure("Required issuerConnector is missing in message");
      }

      String securityProfile = null;
      if (additional.containsKey("securityProfile")) {
        securityProfile = additional.get("securityProfile").toString();
      }

      return verifyTokenIds(jwt, issuerConnector.toString(), securityProfile);

    } else {
      throw new EdcException("Missing required additional information for IDS token validation");
    }
  }

  private Result<SignedJWT> verifyTokenIds(
      SignedJWT jwt, String issuerConnector, @Nullable String securityProfile) {
    try {
      var claims = jwt.getJWTClaimsSet().getClaims();

      // referringConnector (DAT, optional) vs issuerConnector (Message-Header,
      // mandatory)
      var referringConnector = claims.get("referringConnector");

      if (validateReferring && !issuerConnector.equals(referringConnector)) {
        return Result.failure(
            "referingConnector in token does not match issuerConnector in message");
      }

      // securityProfile (DAT, mandatory) vs securityProfile (Message-Payload,
      // optional)
      try {
        var tokenSecurityProfile = claims.get("securityProfile");

        if (securityProfile != null && !securityProfile.equals(tokenSecurityProfile)) {
          return Result.failure(
              "securityProfile in token does not match securityProfile in payload");
        }
      } catch (Exception e) {
        // Nothing to do, payload mostly no connector instance
      }
    } catch (ParseException e) {
      throw new EdcException(
          "IdsValidationRule: unable to parse SignedJWT (" + e.getMessage() + ")");
    }

    return Result.success(jwt);
  }
}
