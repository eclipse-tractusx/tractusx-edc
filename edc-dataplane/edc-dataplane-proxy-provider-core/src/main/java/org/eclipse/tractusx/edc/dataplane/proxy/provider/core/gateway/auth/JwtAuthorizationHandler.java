/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.dataplane.proxy.provider.core.gateway.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.authorization.AuthorizationExtension;
import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.authorization.AuthorizationHandler;

import java.text.ParseException;

import static org.eclipse.edc.spi.result.Result.failure;

/**
 * Authenticates JWTs using a provided verifier and delegates to an {@link AuthorizationExtension} to provide access control checks for the requested path.
 */
public class JwtAuthorizationHandler implements AuthorizationHandler {
    private final JWSVerifier verifier;
    private final AuthorizationExtension authorizationExtension;
    private final Monitor monitor;

    public JwtAuthorizationHandler(JWSVerifier verifier, AuthorizationExtension authorizationExtension, Monitor monitor) {
        this.verifier = verifier;
        this.authorizationExtension = authorizationExtension;
        this.monitor = monitor;
    }

    @Override
    public Result<Void> authorize(String token, String path) {
        try {
            var jwt = SignedJWT.parse(token);
            var result = jwt.verify(verifier);

            if (!result) {
                return failure("Invalid token");
            }

            var claimToken = ClaimToken.Builder.newInstance()
                    .claims(jwt.getJWTClaimsSet().getClaims())
                    .build();

            return authorizationExtension.authorize(claimToken, path);
        } catch (ParseException | JOSEException e) {
            monitor.info("Invalid JWT received: " + e.getMessage());
            return failure("Invalid token");
        }
    }
}
