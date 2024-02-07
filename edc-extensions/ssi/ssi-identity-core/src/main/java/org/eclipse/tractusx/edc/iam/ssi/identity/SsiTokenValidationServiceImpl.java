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

package org.eclipse.tractusx.edc.iam.ssi.identity;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.tractusx.edc.iam.ssi.spi.SsiCredentialClient;
import org.eclipse.tractusx.edc.iam.ssi.spi.SsiTokenValidationService;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SsiTokenValidationServiceImpl implements SsiTokenValidationService {

    private final SsiCredentialClient credentialClient;

    public SsiTokenValidationServiceImpl(SsiCredentialClient credentialClient) {
        this.credentialClient = credentialClient;
    }

    @Override
    public Result<ClaimToken> validate(TokenRepresentation tokenRepresentation, List<TokenValidationRule> rules) {
        return credentialClient.validate(tokenRepresentation)
                .compose(claimToken -> checkRules(claimToken, tokenRepresentation.getAdditional(), rules));
    }

    private Result<ClaimToken> checkRules(ClaimToken claimToken, @Nullable Map<String, Object> additional, List<TokenValidationRule> rules) {
        var errors = rules.stream()
                .map(r -> r.checkRule(claimToken, additional))
                .filter(Result::failed)
                .map(Result::getFailureMessages)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        if (!errors.isEmpty()) {
            return Result.failure(errors);
        }
        return Result.success(claimToken);
    }
}
