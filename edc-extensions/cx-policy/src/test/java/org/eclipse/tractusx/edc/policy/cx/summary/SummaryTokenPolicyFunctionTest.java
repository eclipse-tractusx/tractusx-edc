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

package org.eclipse.tractusx.edc.policy.cx.summary;

import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyNamespaces.CX_SUMMARY_CREDENTIAL;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SummaryTokenPolicyFunctionTest {

    @Test
    void verify_add_credential() {
        var function = new SummaryTokenPolicyFunction();

        var context = mock(PolicyContext.class);
        var builder = TokenParameters.Builder.newInstance().audience("aud");
        when(context.getContextData(eq(TokenParameters.Builder.class))).thenReturn(builder);

        var policy = Policy.Builder.newInstance().build();

        function.apply(policy, context);

        assertThat(builder.build().getAdditional().containsKey(CX_SUMMARY_CREDENTIAL)).isTrue();
    }
}
