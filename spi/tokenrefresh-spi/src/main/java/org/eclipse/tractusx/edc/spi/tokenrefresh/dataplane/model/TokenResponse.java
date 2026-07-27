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

package org.eclipse.tractusx.edc.spi.tokenrefresh.dataplane.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponse(@JsonProperty("access_token") String accessToken,
                            @JsonProperty("refresh_token") String refreshToken,
                            @JsonProperty("expires") Long expiresInLegacy, // TODO: Still needed because older implementations use this non-spec-compliant value, can be removed when only 0.12.x based connectors are in the field
                            @JsonProperty("expires_in") Long expiresIn,
                            @JsonProperty("token_type") String tokenType) {

    public Long expiresInSeconds() {
        // Remove when the expiresInLegacy becomes obsolete, change expiresIn to expiresInSeconds
        return expiresIn() != null ? expiresIn() : expiresInLegacy();
    }
}
