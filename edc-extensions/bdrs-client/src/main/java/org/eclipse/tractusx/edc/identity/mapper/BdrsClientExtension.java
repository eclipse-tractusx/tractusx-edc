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

package org.eclipse.tractusx.edc.identity.mapper;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.core.utils.RequiredConfigWarnings;

import static org.eclipse.tractusx.edc.identity.mapper.BdrsClientExtension.NAME;

@Extension(value = NAME)
public class BdrsClientExtension implements ServiceExtension {
    public static final String NAME = "BPN/DID Resolution Service Client Extension";

    public static final int DEFAULT_BDRS_CACHE_VALIDITY = 15 * 60; // 15 minutes
    @Setting(value = "Base URL of the BDRS service", required = true)
    public static final String BDRS_SERVER_URL_PROPERTY = "tx.iam.iatp.bdrs.server.url";

    @Setting(value = "Validity period in seconds for the cached BPN/DID mappings. After this period a new resolution request will hit the server.", defaultValue = DEFAULT_BDRS_CACHE_VALIDITY + "")
    public static final String BDRS_SERVER_CACHE_VALIDITY_PERIOD = "tx.iam.iatp.bdrs.cache.validity";

    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public AudienceResolver getBdrsAudienceResolver(ServiceExtensionContext context) {
        var baseUrl = context.getConfig().getString(BDRS_SERVER_URL_PROPERTY, null);
        if (baseUrl == null) {
            RequiredConfigWarnings.warningNotPresent(context.getMonitor(), BDRS_SERVER_URL_PROPERTY);
        }
        var cacheValidity = context.getConfig().getInteger(BDRS_SERVER_CACHE_VALIDITY_PERIOD, DEFAULT_BDRS_CACHE_VALIDITY);
        return new BdrsClient(baseUrl, cacheValidity, httpClient, context.getMonitor(), typeManager.getMapper());
    }

}
