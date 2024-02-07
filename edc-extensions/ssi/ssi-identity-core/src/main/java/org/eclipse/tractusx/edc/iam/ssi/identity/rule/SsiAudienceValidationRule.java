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

package org.eclipse.tractusx.edc.iam.ssi.identity.rule;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;

public class SsiAudienceValidationRule implements TokenValidationRule {

    private final String endpointAudience;

    public SsiAudienceValidationRule(String endpointAudience) {
        this.endpointAudience = endpointAudience;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        var audiences = toVerify.getListClaim(AUDIENCE);
        if (audiences.isEmpty()) {
            return Result.failure("Required audience (aud) claim is missing in token");
        } else if (!audiences.contains(endpointAudience)) {
            return Result.failure("Token audience (aud) claim did not contain audience: " + endpointAudience);
        }
        return Result.success();
    }
}
