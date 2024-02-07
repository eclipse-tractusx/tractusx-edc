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

package org.eclipse.tractusx.edc.iam.ssi.miw;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.iam.ssi.miw.config.SsiMiwConfiguration;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static java.lang.String.format;
import static org.eclipse.tractusx.edc.iam.ssi.miw.utils.PathUtils.removeTrailingSlash;


@Extension(SsiMiwConfigurationExtension.EXTENSION_NAME)
public class SsiMiwConfigurationExtension implements ServiceExtension {


    @Setting(value = "MIW API base url")
    public static final String MIW_BASE_URL = "tx.ssi.miw.url";
    @Setting(value = "MIW Authority ID")
    public static final String MIW_AUTHORITY_ID = "tx.ssi.miw.authority.id";
    @Setting(value = "MIW Authority Issuer")
    public static final String MIW_AUTHORITY_ISSUER = "tx.ssi.miw.authority.issuer";
    public static final String AUTHORITY_ID_TEMPLATE = "did:web:%s:%s";
    protected static final String EXTENSION_NAME = "SSI Miw configuration extension";

    @Provider
    public SsiMiwConfiguration miwConfiguration(ServiceExtensionContext context) {
        var baseUrl = removeTrailingSlash(context.getConfig().getString(MIW_BASE_URL));
        var authorityId = context.getConfig().getString(MIW_AUTHORITY_ID);
        var authorityIssuer = authorityIssuer(context, baseUrl, authorityId);

        return SsiMiwConfiguration.Builder.newInstance()
                .url(baseUrl)
                .authorityId(authorityId)
                .authorityIssuer(authorityIssuer)
                .build();
    }


    private String authorityIssuer(ServiceExtensionContext context, String baseUrl, String authorityId) {
        var uri = URI.create(baseUrl);
        var defaultAuthorityIssuer = format(AUTHORITY_ID_TEMPLATE, URLEncoder.encode(uri.getAuthority(), StandardCharsets.UTF_8), authorityId);
        return context.getConfig().getString(MIW_AUTHORITY_ISSUER, defaultAuthorityIssuer);
    }
}
