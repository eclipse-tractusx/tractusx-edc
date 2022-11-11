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
package org.eclipse.tractusx.edc.ssi.miw;

import java.util.ArrayList;
import java.util.Arrays;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.ssi.miw.registry.VerifiableCredentialRegistry;
import org.eclipse.tractusx.edc.ssi.miw.registry.VerifiableCredentialRegistryImpl;
import org.eclipse.tractusx.edc.ssi.miw.wallet.ManagedIdentityWalletApiServiceImpl;
import org.eclipse.tractusx.edc.ssi.miw.wallet.ManagedIdentityWalletConfig;
import org.eclipse.tractusx.edc.ssi.spi.IdentityWalletApiService;

@Provides({IdentityWalletApiService.class, VerifiableCredentialRegistry.class})
public class ManagedIdentityWalletExtension implements ServiceExtension {

  @Override
  public String name() {
    return "Managed Identity Wallets";
  }

  @Inject WebService webService;
  @Inject private OkHttpClient okHttpClient;

  private static final String LOG_PREFIX_SETTING = "ssi.miw.logprefix";

  /** Keycloak Settings */
  private static final String KEYCLOAK_CLIENT_ID = "ssi.miw.keycloak.clientid";

  private static final String KEYCLOAK_CLIENT_SECRET = "ssi.miw.keycloak.clientsecret";
  private static final String KEYCLOAK_CLIENT_GRAND_TYPE = "ssi.miw.keycloak.grandtype";
  private static final String KEYCLOAK_SCOPE = "ssi.miw.keycloak.scope";

  /** Access Token Settings */
  private static final String ACCESSTOKEN_URL = "ssi.miw.accesstoken.url";

  /** Connection Settings */
  private static final String WALLET_URL = "ssi.miw.url";

  private static final String WALLET_DID = "ssi.miw.did";
  private static final String OWNER_BPN = "ssi.miw.bpn";
  private static final String DIDS_OF_TRUSTED_PROVIDERS = "ssi.miw.trusted.providers";

  @Inject DataManagementApiConfiguration config;

  private ManagedIdentityWalletConfig walletConfig;

  @Override
  public void initialize(ServiceExtensionContext context) {
    var logPrefix = context.getSetting(LOG_PREFIX_SETTING, "MIW");
    var typeManager = context.getTypeManager();

    ManagedIdentityWalletConfig.Builder walletBuilderConfig =
        ManagedIdentityWalletConfig.Builder.newInstance();
    this.walletConfig =
        walletBuilderConfig
            .walletURL(context.getConfig().getString(WALLET_URL))
            .walletDID(context.getConfig().getString(WALLET_DID))
            .accessTokenURL(context.getConfig().getString(ACCESSTOKEN_URL))
            .keycloakClientID(context.getConfig().getString(KEYCLOAK_CLIENT_ID))
            .keycloakClientSecret(context.getConfig().getString(KEYCLOAK_CLIENT_SECRET))
            .keycloakGrandType(context.getConfig().getString(KEYCLOAK_CLIENT_GRAND_TYPE))
            .keycloakScope(context.getConfig().getString(KEYCLOAK_SCOPE))
            .ownerBPN(context.getConfig().getString(OWNER_BPN))
            .trustedProvider(
                new ArrayList<>(
                    Arrays.asList(
                        (context.getConfig().getString(DIDS_OF_TRUSTED_PROVIDERS)).split(";"))))
            .build();

    VerifiableCredentialRegistry credentialRegistry = new VerifiableCredentialRegistryImpl();
    context.registerService(VerifiableCredentialRegistry.class, credentialRegistry);
    context.registerService(
        IdentityWalletApiService.class,
        new ManagedIdentityWalletApiServiceImpl(
            context.getMonitor(),
            logPrefix,
            walletConfig,
            okHttpClient,
            typeManager,
            credentialRegistry));
  }
}
