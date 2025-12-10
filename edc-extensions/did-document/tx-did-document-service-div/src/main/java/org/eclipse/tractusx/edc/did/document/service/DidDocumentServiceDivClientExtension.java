/********************************************************************************
 * Copyright (c) 2025 SAP SE
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

package org.eclipse.tractusx.edc.did.document.service;

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.iam.dcp.sts.dim.oauth.DimOauth2Client;
import org.eclipse.tractusx.edc.spi.did.document.service.DidDocumentServiceClient;

import java.util.Optional;

@Provides(DidDocumentServiceClient.class)
public class DidDocumentServiceDivClientExtension implements ServiceExtension {

    @Inject
    private DidResolverRegistry resolverRegistry;

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private DimOauth2Client dimOauth2Client;

    @Inject
    private TypeManager typeManager;

    @Inject
    private Monitor monitor;

    @Setting(key = "tx.edc.iam.sts.dim.url", description = "STS Dim endpoint", required = false)
    private String divUrl;

    @Setting(key = "edc.iam.issuer.id", description = "EDC Issuer Id")
    private String ownDid;

    @Override
    public void initialize(ServiceExtensionContext context) {

        Optional.ofNullable(divUrl)
                .map(divUrl -> new DidDocumentServiceDivClient(
                        resolverRegistry,
                        httpClient,
                        dimOauth2Client,
                        typeManager.getMapper(),
                        divUrl,
                        ownDid,
                        monitor)
                ).ifPresentOrElse(client ->
                                context.registerService(DidDocumentServiceClient.class, client),
                        () -> monitor.warning("DIV Url not configured, DidDocumentServiceDIVClient will not be registered")
                );
    }
}
