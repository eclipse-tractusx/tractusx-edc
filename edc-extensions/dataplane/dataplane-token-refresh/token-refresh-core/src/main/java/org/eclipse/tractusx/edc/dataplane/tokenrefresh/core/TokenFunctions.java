/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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
 */

package org.eclipse.tractusx.edc.dataplane.tokenrefresh.core;

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;

public class TokenFunctions {

    /**
     * Returns the "jti" claim of a JWT in serialized format. Will throw a {@link RuntimeException} if the token is not in valid
     * serialized JWT format.
     */
    public static @Nullable String getTokenId(String serializedJwt) {
        try {
            return SignedJWT.parse(serializedJwt).getJWTClaimsSet().getStringClaim(JwtRegisteredClaimNames.JWT_ID);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
