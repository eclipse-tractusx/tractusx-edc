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

package org.eclipse.tractusx.edc.iam.iatp.scope;

import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DefaultScopeExtractorTest {

    private static final Set<String> SCOPES = Set.of("scope1", "scope2");
    private DefaultScopeExtractor extractor;

    @BeforeEach
    void setup() {
        extractor = new DefaultScopeExtractor(SCOPES);
    }

    @Test
    void verify_applyExtractor() {
        var builder = TokenParameters.Builder.newInstance();
        var ctx = PolicyContextImpl.Builder.newInstance().additional(TokenParameters.Builder.class, builder).build();
        extractor.apply(Policy.Builder.newInstance().build(), ctx);

        assertThat(builder.build().getScope().split(" ")).contains("scope1", "scope2");
    }

    @Test
    void verify_applyExtractorFails_whenTokenParamsBuilderMissing() {
        var ctx = PolicyContextImpl.Builder.newInstance().build();
        assertThatThrownBy(() -> extractor.apply(Policy.Builder.newInstance().build(), ctx))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("not set in policy context");

    }
}
