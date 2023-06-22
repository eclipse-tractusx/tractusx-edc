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
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.authorization.AuthorizationExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthorizationHandlerTest {
    private JwtAuthorizationHandler handler;
    private AuthorizationExtension authExtension;
    private JWSVerifier verifier;


    @BeforeEach
    void setUp() {
        verifier = mock(JWSVerifier.class);
        Monitor monitor = mock(Monitor.class);
        authExtension = mock(AuthorizationExtension.class);
        handler = new JwtAuthorizationHandler(verifier, authExtension, monitor);
    }

    @Test
    void verify_validCase() throws JOSEException {
        when(verifier.verify(any(), any(), any())).thenReturn(true);
        when(authExtension.authorize(isA(ClaimToken.class), eq("foo"))).thenReturn(success());

        var result = handler.authorize(TestTokens.TEST_TOKEN, "foo");

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void verify_parseInValidToken() throws JOSEException {
        when(verifier.verify(any(), any(), any())).thenReturn(false);

        var result = handler.authorize(TestTokens.TEST_TOKEN, "foo");

        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void verify_notAuthorized() throws JOSEException {
        when(verifier.verify(any(), any(), any())).thenReturn(true);
        when(authExtension.authorize(isA(ClaimToken.class), eq("foo"))).thenReturn(failure("Not authorized"));

        var result = handler.authorize(TestTokens.TEST_TOKEN, "foo");

        assertThat(result.succeeded()).isFalse();

        verify(authExtension).authorize(isA(ClaimToken.class), eq("foo"));
    }


}
