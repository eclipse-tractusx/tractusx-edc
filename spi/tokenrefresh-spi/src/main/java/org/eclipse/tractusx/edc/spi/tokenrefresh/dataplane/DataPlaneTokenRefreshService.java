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

package org.eclipse.tractusx.edc.spi.tokenrefresh.dataplane;

import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.spi.tokenrefresh.dataplane.model.TokenResponse;

/**
 * This service receives an incoming token refresh request, validates it and generates a new token pair (access token + refresh token).
 */
public interface DataPlaneTokenRefreshService {
    /**
     * Generates a new token pair (access and refresh token) based on an existing refresh token and access token.
     *
     * @param refreshToken        The refresh token that was issued in the original/previous token request.
     * @param authenticationToken A <a href="https://github.com/eclipse-tractusx/tractusx-profiles/blob/main/tx/refresh/refresh.token.grant.profile.md#31-client-authentication">client authentication token</a>
     * @return A result that contains the new access and refresh token, or a failure.
     */
    Result<TokenResponse> refreshToken(String refreshToken, String authenticationToken);
}
