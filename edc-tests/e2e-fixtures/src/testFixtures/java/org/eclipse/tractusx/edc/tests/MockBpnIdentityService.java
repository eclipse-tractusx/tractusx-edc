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

package org.eclipse.tractusx.edc.tests;

import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.Map;

import static java.lang.String.format;

/**
 * An {@link IdentityService} that will inject the BPN claim in every token.
 * Please only use in testing scenarios!
 */
public class MockBpnIdentityService implements IdentityService {

    private static final String BUSINESS_PARTNER_NUMBER_CLAIM = "BusinessPartnerNumber";
    private final String businessPartnerNumber;
    private final TypeManager typeManager = new JacksonTypeManager();

    public MockBpnIdentityService(String businessPartnerNumber) {
        this.businessPartnerNumber = businessPartnerNumber;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
        var token = Map.of(BUSINESS_PARTNER_NUMBER_CLAIM, businessPartnerNumber);

        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(typeManager.writeValueAsString(token))
                .build();
        return Result.success(tokenRepresentation);
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, VerificationContext verificationContext) {

        var token = typeManager.readValue(tokenRepresentation.getToken(), Map.class);
        if (token.containsKey(BUSINESS_PARTNER_NUMBER_CLAIM)) {
            return Result.success(ClaimToken.Builder.newInstance()
                    .claim(BUSINESS_PARTNER_NUMBER_CLAIM, token.get(BUSINESS_PARTNER_NUMBER_CLAIM))
                    .build());
        }
        return Result.failure(format("Expected %s claim, but token did not contain them", BUSINESS_PARTNER_NUMBER_CLAIM));
    }

}
