/*
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.edc.ssi.core;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.ssi.core.claims.SSIVerifiableCredentials;
import org.eclipse.tractusx.edc.ssi.core.claims.SSIVerifiableCredentialsImpl;
import org.eclipse.tractusx.edc.ssi.core.claims.SSIVerifiablePresentation;
import org.eclipse.tractusx.edc.ssi.core.claims.SSIVerifiablePresentationImpl;
import org.eclipse.tractusx.edc.ssi.miw.registry.VerifiableCredentialRegistry;
import org.eclipse.tractusx.edc.ssi.spi.IdentityWalletApiService;

@Provides({IdentityService.class, SSIVerifiableCredentials.class, SSIVerifiablePresentation.class})
public class SSIIdentityServiceExtension implements ServiceExtension {

  @Override
  public String name() {
    return "SSI Identity Service";
  }

  @Inject WebService webService;

  @Inject private OkHttpClient okHttpClient;

  @Inject public static IdentityWalletApiService walletApiService;

  public SSIVerifiablePresentation verifiablePresentation;

  public SSIVerifiableCredentials verifiableCredentials;

  @Inject public VerifiableCredentialRegistry verifiableCredentialRegistry;

  private static final String LOG_PREFIX_SETTING = "ssi.miw.logprefix";

  @Inject DataManagementApiConfiguration config;

  @Override
  public void initialize(ServiceExtensionContext context) {
    var logPrefix = context.getSetting(LOG_PREFIX_SETTING, "MIW");
    var typeManager = context.getTypeManager();
    context.getMonitor().debug("Starting initializing of SSI Identity Service");

    verifiableCredentials =
        new SSIVerifiableCredentialsImpl(walletApiService, verifiableCredentialRegistry);
    verifiablePresentation = new SSIVerifiablePresentationImpl(walletApiService);
    context.registerService(VerifiableCredentialRegistry.class, verifiableCredentialRegistry);
    context.registerService(SSIVerifiableCredentials.class, verifiableCredentials);
    context.registerService(SSIVerifiablePresentation.class, verifiablePresentation);
    context.registerService(
        IdentityService.class,
        new SSIIdentityServiceImpl(
            walletApiService, verifiableCredentials, verifiablePresentation));
  }
}
