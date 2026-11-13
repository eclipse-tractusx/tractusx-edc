/********************************************************************************
 * Copyright (c) 2026 Cofinity-X GmbH
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.edc.verifiablecredentials.linkeddata;

import com.apicatalog.ld.DocumentError;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DidMethodResolverTest {

    private static final String DID = "did:web:issuer";

    private final DidResolverRegistry resolverRegistry = mock();
    private final DidMethodResolver resolver = new DidMethodResolver(resolverRegistry);

    @Test
    void resolve_selectsMethodById() throws DocumentError, JOSEException {
        didDocument(method(DID + "#key-1"), method(DID + "#key-2"));

        var method = resolver.resolve(URI.create(DID + "#key-2"), null, null);

        assertThat(method.id()).isEqualTo(URI.create(DID + "#key-2"));
        verify(resolverRegistry).resolve(DID);
    }

    @Test
    void resolve_relativeMethodId() throws DocumentError, JOSEException {
        didDocument(method("#key-1"), method("#key-2"));

        var method = resolver.resolve(URI.create(DID + "#key-2"), null, null);

        assertThat(method.id()).isEqualTo(URI.create(DID + "#key-2"));
    }

    @Test
    void resolve_noFragment_singleMethod() throws DocumentError, JOSEException {
        didDocument(method(DID + "#key-1"));

        var method = resolver.resolve(URI.create(DID), null, null);

        assertThat(method.id()).isEqualTo(URI.create(DID + "#key-1"));
    }

    @Test
    void resolve_noFragment_multipleMethods_shouldFail() throws JOSEException {
        didDocument(method(DID + "#key-1"), method(DID + "#key-2"));

        assertThatThrownBy(() -> resolver.resolve(URI.create(DID), null, null)).isInstanceOf(DocumentError.class);
    }

    @Test
    void resolve_methodNotFound_shouldFail() throws JOSEException {
        didDocument(method(DID + "#key-1"));

        assertThatThrownBy(() -> resolver.resolve(URI.create(DID + "#key-2"), null, null)).isInstanceOf(DocumentError.class);
    }

    @Test
    void resolve_methodOfOtherDid_shouldFail() throws JOSEException {
        didDocument(method("did:web:other#key-1"));

        assertThatThrownBy(() -> resolver.resolve(URI.create(DID + "#key-1"), null, null)).isInstanceOf(DocumentError.class);
    }

    @Test
    void resolve_duplicateMethodId_shouldFail() throws JOSEException {
        didDocument(method(DID + "#key-1"), method("#key-1"));

        assertThatThrownBy(() -> resolver.resolve(URI.create(DID + "#key-1"), null, null)).isInstanceOf(DocumentError.class);
    }

    @Test
    void resolve_noVerificationMethods_shouldFail() {
        didDocument();

        assertThatThrownBy(() -> resolver.resolve(URI.create(DID + "#key-1"), null, null)).isInstanceOf(DocumentError.class);
    }

    private void didDocument(VerificationMethod... methods) {
        var document = DidDocument.Builder.newInstance().id(DID).verificationMethod(List.of(methods)).build();
        when(resolverRegistry.resolve(anyString())).thenReturn(Result.success(document));
    }

    private VerificationMethod method(String id) throws JOSEException {
        var key = new ECKeyGenerator(Curve.P_256).keyID(id).generate().toPublicJWK();
        return VerificationMethod.Builder.newInstance()
                .id(id)
                .type("JsonWebKey2020")
                .controller(DID)
                .publicKeyJwk(key.toJSONObject())
                .build();
    }
}
