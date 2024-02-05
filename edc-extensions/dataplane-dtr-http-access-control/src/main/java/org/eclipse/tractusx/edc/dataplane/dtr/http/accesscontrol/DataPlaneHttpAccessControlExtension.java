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

import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParamsProvider;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;
import org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.client.DtrAccessVerificationClient;
import org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.client.DtrOauth2TokenClient;
import org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.client.HttpAccessVerificationClient;
import org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.client.Oauth2TokenClient;

import java.util.HashMap;
import java.util.Map;

@Extension(value = "Data Plane HTTP Access Control")
public class DataPlaneHttpAccessControlExtension implements ServiceExtension {

    @Setting(value = "Custom setting to allow configuration of error handler controller ports.")
    public static final String ERROR_ENDPOINT_PORT = "edc.granular.access.verification.error.endpoint.port";
    @Setting(value = "Contains the base URL of the EDC data plane endpoint where the data plane requests are sent by the end users.")
    public static final String EDC_DATA_PLANE_BASE_URL = "edc.granular.access.verification.edc.data.plane.baseUrl";
    @Setting(value = "Comma separated list of DTR configuration names used as keys for DTR clients.")
    public static final String EDC_DTR_CONFIG_NAMES = "edc.granular.access.verification.dtr.names";
    /**
     * Prefix for individual DTR configurations.
     */
    public static final String EDC_DTR_CONFIG_PREFIX = "edc.granular.access.verification.dtr.config.";
    /**
     * Configuration property suffix for the configuration of DTR decision cache. The cache is turned off if set to 0.
     */
    public static final String DTR_DECISION_CACHE_MINUTES = "dtr.decision.cache.duration.minutes";
    /**
     * Configuration property suffix for the pattern to allow for the recognition of aspect model requests which need
     * to be handled by DTR access control.
     */
    public static final String ASPECT_MODEL_URL_PATTERN = "aspect.model.url.pattern";
    /**
     * Configuration property suffix for the URL where DTR can be reached.
     */
    public static final String DTR_ACCESS_VERIFICATION_URL = "dtr.access.verification.endpoint.url";
    /**
     * Configuration property suffix for the URL where OAUTH2 tokens can be obtained for the DTR requests.
     */
    public static final String OAUTH2_TOKEN_ENDPOINT_URL = "oauth2.token.endpoint.url";
    /**
     * Configuration property suffix for the scope we need to use for OAUTH2 token requests when we need to access DTR.
     */
    public static final String OAUTH2_TOKEN_SCOPE = "oauth2.token.scope";
    /**
     * Configuration property suffix for the client id we need to use for OAUTH2 token requests when we need to access DTR.
     */
    public static final String OAUTH2_TOKEN_CLIENT_ID = "oauth2.token.clientId";

    /**
     * Configuration property suffix for the path where we can find the client secret in vault for the OAUTH2 token requests when we need to access DTR.
     */
    public static final String OAUTH2_TOKEN_CLIENT_SECRET_PATH = "oauth2.token.clientSecret.path";
    public static final int DEFAULT_PORT = 9054;

    public static final String ERROR = "error";

    @Inject
    private WebService webService;
    @Inject
    private WebServer webServer;
    @Inject
    private WebServiceConfigurer configurer;
    @Inject
    private HttpRequestParamsProvider paramsProvider;
    @Inject
    private Monitor monitor;
    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private TypeManager typeManager;
    @Inject
    private Vault vault;

    @Override
    public String name() {
        return "Data Plane HTTP Access Control";
    }

    @Override
    public void initialize(final ServiceExtensionContext context) {
        final var config = new HttpAccessControlCheckClientConfig(context);
        if (config.getDtrClientConfigMap().isEmpty()) {
            //turn off the extension if no DTR is configured
            monitor.warning(EDC_DTR_CONFIG_NAMES + " is not configured, DTR access control is turned OFF.");
            return;
        }
        final Map<String, HttpAccessVerificationClient> dtrClients = new HashMap<>();
        config.getDtrClientConfigMap().forEach((k, v) -> {
            final Oauth2TokenClient tokenClient = new DtrOauth2TokenClient(monitor, httpClient, typeManager, vault, v);
            final HttpAccessVerificationClient client = new DtrAccessVerificationClient(monitor, httpClient, tokenClient, typeManager, config, v);
            dtrClients.put(k, client);
        });
        final var httpAclParamsDecorator = new HttpAccessControlRequestParamsDecorator(monitor, dtrClients, config);
        paramsProvider.registerSinkDecorator(httpAclParamsDecorator);
        paramsProvider.registerSourceDecorator(httpAclParamsDecorator);

        configurer.configure(context, webServer, createApiContext(config.getErrorEndpointPort()));
        webService.registerResource(ERROR, new HttpAccessControlErrorApiController());
    }

    private WebServiceSettings createApiContext(final int port) {
        return WebServiceSettings.Builder.newInstance()
                .apiConfigKey("web.http." + ERROR)
                .contextAlias(ERROR)
                .defaultPath("/error")
                .defaultPort(port)
                .name(ERROR)
                .build();
    }
}
