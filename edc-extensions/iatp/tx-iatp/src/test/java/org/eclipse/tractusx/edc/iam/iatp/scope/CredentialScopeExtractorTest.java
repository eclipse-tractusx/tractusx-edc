/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.iam.iatp.scope;

import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.tractusx.edc.edr.spi.CoreConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.iam.iatp.TxIatpConstants.CREDENTIAL_TYPE_NAMESPACE;
import static org.eclipse.tractusx.edc.iam.iatp.scope.CredentialScopeExtractor.FRAMEWORK_CREDENTIAL_PREFIX;

public class CredentialScopeExtractorTest {

    private CredentialScopeExtractor extractor;

    @BeforeEach
    void setup() {
        extractor = new CredentialScopeExtractor();
    }

    @Test
    void verify_extractScopes() {
        var builder = TokenParameters.Builder.newInstance();
        var ctx = PolicyContextImpl.Builder.newInstance().additional(TokenParameters.Builder.class, builder).build();
        var scopes = extractor.extractScopes(CoreConstants.TX_CREDENTIAL_NAMESPACE + FRAMEWORK_CREDENTIAL_PREFIX + "pfc", null, null, ctx);
        assertThat(scopes).contains(CREDENTIAL_TYPE_NAMESPACE + ":PfcCredential:read");
    }

    @Test
    void verify_extractScope_Empty() {
        var builder = TokenParameters.Builder.newInstance();
        var ctx = PolicyContextImpl.Builder.newInstance().additional(TokenParameters.Builder.class, builder).build();
        var scopes = extractor.extractScopes("wrong", null, null, ctx);
        assertThat(scopes).isEmpty();
    }
}
