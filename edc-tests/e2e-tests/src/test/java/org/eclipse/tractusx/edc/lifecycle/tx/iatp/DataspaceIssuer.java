/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.lifecycle.tx.iatp;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.junit.testfixtures.TestUtils;

import java.util.List;

/**
 * Dataspace issuer configurations
 */
public class DataspaceIssuer {

    public static final String DATASPACE_ISSUER = "did:example:dataspace_issuer";

    private static final String KEY_ID = "#key1";

    private final DidDocument didDocument;

    public DataspaceIssuer() {
        didDocument = generateDidDocument();
    }

    public String didUrl() {
        return DATASPACE_ISSUER;
    }

    public DidDocument didDocument() {
        return didDocument;
    }

    public String verificationId() {
        return DATASPACE_ISSUER + KEY_ID;
    }

    private DidDocument generateDidDocument() {
        var key = TestUtils.getResourceFileContentAsString("ec-p256-public.pem");
        try {
            var jwk = JWK.parseFromPEMEncodedObjects(key);
            var verificationMethod = VerificationMethod.Builder.newInstance()
                    .id(verificationId())
                    .controller(didUrl())
                    .type("JsonWebKey2020")
                    .publicKeyJwk(jwk.toPublicJWK().toJSONObject())
                    .build();

            return DidDocument.Builder.newInstance()
                    .id(didUrl())
                    .authentication(List.of(KEY_ID))
                    .verificationMethod(List.of(verificationMethod))
                    .build();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }

    }
}
