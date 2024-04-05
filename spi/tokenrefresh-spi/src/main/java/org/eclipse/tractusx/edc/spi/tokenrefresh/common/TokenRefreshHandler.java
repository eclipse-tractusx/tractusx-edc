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

package org.eclipse.tractusx.edc.spi.tokenrefresh.common;

import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * Handles token refreshing against an OAuth2-compliant token refresh endpoint.
 */
public interface TokenRefreshHandler {
    /**
     * Refreshes a token identified by the token ID and returns the refreshed token.
     *
     * @param tokenId The ID of the token, e.g. a {@code jti} claim in JWT tokens.
     * @return An updated access+refresh token pair.
     */
    ServiceResult<DataAddress> refreshToken(String tokenId);

    /**
     * Refreshes a token identified by the token ID and returns the refreshed token.
     *
     * @param tokenId The ID of the token, e.g. a {@code jti} claim in JWT tokens.
     * @param edr     The {@link DataAddress} containing the EDR
     * @return An updated access+refresh token pair.
     */
    ServiceResult<DataAddress> refreshToken(String tokenId, DataAddress edr);
}
