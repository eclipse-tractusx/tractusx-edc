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

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Tokens for testing.
 */
public class TestTokens {
    private static final String DELIMITER = "-----";
    private static final String HEADER = DELIMITER + "BEGIN" + " PUBLIC " + "KEY" + DELIMITER + "\n";
    private static final String FOOTER = "\n" + DELIMITER + "END" + " PUBLIC " + "KEY" + DELIMITER + "\n";

    public static final String TEST_TOKEN = "eyJhbGciOiJSUzI1NiIsInZlcnNpb24iOnRydWV9.eyJpc3MiOiJ0ZXN0LWNvbm5lY3RvciIsInN1YiI6ImNvbnN1bWVyLWNvbm5lY3RvciIsImF1ZCI6InRlc3QtY29ubmVjdG9yIiwiaWF0IjoxNjgxOTEzNjM2LCJleHAiOjMzNDU5NzQwNzg4LCJjaWQiOiIzMmE2M2E3ZC04MGQ2LTRmMmUtOTBlNi04MGJhZjVmYzJiM2MifQ.QAuotoRxpEqfuzkTcTq2w5Tcyy3Rc3UzUjjvNc_zwgNROGLe-wO9tFET1dJ_I5BttRxkngDS37dS4R6lN5YXaGHgcH2rf_FuVcJUSFqTp_usGAcx6m7pQQwqpNdcYgmq0NJp3xP87EFPHAy4kBxB5bqpmx4J-zrj9U_gerZ2WlRqpu0SdgP0S5v5D1Gm-vYkLqgvsugrAWH3Ti7OjC5UMdj0kDFwro2NpMY8SSNryiVvBEv8hn0KZdhhebIqPdhqbEQZ9d8WKzcgoqQ3DBd4ijzkd3Fz7ADD2gy_Hxn8Hi2LcItuB514TjCxYAncTNqZC_JSFEyuxwcGFVz3LdSXgw";


    public static String generatePublic() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            var pair = generator.generateKeyPair();
            var encoded = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
            return HEADER + encoded + FOOTER;
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

}
