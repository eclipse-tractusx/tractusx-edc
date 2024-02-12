/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.iam.ssi.identity;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.tractusx.edc.iam.ssi.spi.SsiCredentialClient;
import org.eclipse.tractusx.edc.iam.ssi.spi.SsiTokenValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class SsiTokenValidationServiceImplTest {

    SsiCredentialClient credentialClient = mock(SsiCredentialClient.class);

    SsiTokenValidationService validationService;

    @BeforeEach
    void setup() {
        validationService = new SsiTokenValidationServiceImpl(credentialClient);
    }

    @Test
    void validate_success() {
        var token = TokenRepresentation.Builder.newInstance().token("test").build();
        var rule = mock(TokenValidationRule.class);
        var claim = ClaimToken.Builder.newInstance().build();

        when(credentialClient.validate(token)).thenReturn(Result.success(claim));
        when(rule.checkRule(any(), any())).thenReturn(Result.success());

        var result = validationService.validate(token, List.of(rule));

        assertThat(result).isNotNull().extracting(Result::getContent).isEqualTo(claim);

        verify(credentialClient).validate(token);
        verify(rule).checkRule(eq(claim), any());
    }

    @Test
    void validate_fail_whenClientFails() {
        var token = TokenRepresentation.Builder.newInstance().token("test").build();
        var rule = mock(TokenValidationRule.class);

        when(credentialClient.validate(token)).thenReturn(Result.failure("failure"));
        when(rule.checkRule(any(), any())).thenReturn(Result.success());

        var result = validationService.validate(token, List.of(rule));

        assertThat(result).isNotNull().matches(Result::failed);

        verify(credentialClient).validate(token);
        verifyNoInteractions(rule);
    }

    @Test
    void validate_fail_whenRuleFails() {
        var token = TokenRepresentation.Builder.newInstance().token("test").build();
        var rule = mock(TokenValidationRule.class);
        var claim = ClaimToken.Builder.newInstance().build();


        when(credentialClient.validate(token)).thenReturn(Result.success(claim));
        when(rule.checkRule(any(), any())).thenReturn(Result.failure("failure"));

        var result = validationService.validate(token, List.of(rule));

        assertThat(result).isNotNull().matches(Result::failed);

        verify(credentialClient).validate(token);
        verify(rule).checkRule(eq(claim), any());
    }
}
