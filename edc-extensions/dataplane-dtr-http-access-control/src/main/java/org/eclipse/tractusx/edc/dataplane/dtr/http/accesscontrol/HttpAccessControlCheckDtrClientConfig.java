/********************************************************************************
 * Copyright (c) 2024 Robert Bosch Manufacturing Solutions GmbH and others
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol;

import org.eclipse.edc.spi.system.configuration.Config;

import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.ASPECT_MODEL_URL_PATTERN;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.DTR_ACCESS_VERIFICATION_URL;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.DTR_DECISION_CACHE_MINUTES;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.OAUTH2_TOKEN_CLIENT_ID;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.OAUTH2_TOKEN_CLIENT_SECRET_PATH;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.OAUTH2_TOKEN_ENDPOINT_URL;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.OAUTH2_TOKEN_SCOPE;


public class HttpAccessControlCheckDtrClientConfig {

    private final String aspectModelUrlPattern;
    private final String dtrAccessVerificationUrl;
    private final String oauth2TokenEndpointUrl;
    private final String oauth2TokenScope;
    private final String oauth2ClientId;
    private final String oauth2ClientSecretPath;
    private final int decisionCacheDurationMinutes;

    public HttpAccessControlCheckDtrClientConfig(final Config context) {
        aspectModelUrlPattern = context.getString(ASPECT_MODEL_URL_PATTERN, null);
        dtrAccessVerificationUrl = context.getString(DTR_ACCESS_VERIFICATION_URL, null);
        oauth2TokenEndpointUrl = context.getString(OAUTH2_TOKEN_ENDPOINT_URL, null);
        oauth2TokenScope = context.getString(OAUTH2_TOKEN_SCOPE, null);
        oauth2ClientId = context.getString(OAUTH2_TOKEN_CLIENT_ID, null);
        oauth2ClientSecretPath = context.getString(OAUTH2_TOKEN_CLIENT_SECRET_PATH, null);
        decisionCacheDurationMinutes = context.getInteger(DTR_DECISION_CACHE_MINUTES, 0);
    }

    public String getAspectModelUrlPattern() {
        return aspectModelUrlPattern;
    }

    public String getDtrAccessVerificationUrl() {
        return dtrAccessVerificationUrl;
    }

    public String getOauth2TokenEndpointUrl() {
        return oauth2TokenEndpointUrl;
    }

    public String getOauth2TokenScope() {
        return oauth2TokenScope;
    }

    public String getOauth2ClientId() {
        return oauth2ClientId;
    }

    public String getOauth2ClientSecretPath() {
        return oauth2ClientSecretPath;
    }

    public int getDecisionCacheDurationMinutes() {
        return decisionCacheDurationMinutes;
    }
}
