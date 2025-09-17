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

package org.eclipse.tractusx.edc.edr.spi;


import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public final class CoreConstants {

    public static final String TX_PREFIX = "tx";
    public static final String TX_AUTH_PREFIX = "tx-auth";
    public static final String CX_POLICY_PREFIX = "cx-policy";
    public static final String TX_NAMESPACE = "https://w3id.org/tractusx/v0.0.1/ns/";
    public static final String TX_AUTH_NS = "https://w3id.org/tractusx/auth/";
    public static final String EDC_CONTEXT = "https://w3id.org/edc/v0.0.1";
    public static final String CX_CREDENTIAL_NS = "https://w3id.org/catenax/credentials/";

    @Deprecated(since = "0.11.0")
    public static final String CX_POLICY_NS = "https://w3id.org/catenax/policy/";
    public static final String CX_POLICY_2025_09_NS = "https://w3id.org/catenax/2025/9/policy/";

    // constants related to token refresh/renewal
    public static final String EDR_PROPERTY_AUTHORIZATION = EDC_NAMESPACE + "authorization";
    public static final String EDR_PROPERTY_REFRESH_TOKEN = TX_AUTH_NS + "refreshToken";
    public static final String EDR_PROPERTY_REFRESH_ENDPOINT = TX_AUTH_NS + "refreshEndpoint";
    public static final String EDR_PROPERTY_REFRESH_AUDIENCE = TX_AUTH_NS + "refreshAudience";
    public static final String AUDIENCE_PROPERTY = TX_AUTH_NS + "audience";
    public static final String EDR_PROPERTY_EXPIRES_IN = TX_AUTH_NS + "expiresIn";

    private CoreConstants() {
    }
}
