/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *      ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.iam.ssi;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.dataspaceconnector.iam.ssi.model.VerifiableCredentialRegistry;
import org.eclipse.dataspaceconnector.iam.ssi.model.VerifiableCredentialRegistryImpl;
import org.eclipse.dataspaceconnector.iam.ssi.wallet.ManagedIdentityWalletApiServiceImpl;
import org.eclipse.dataspaceconnector.iam.ssi.wallet.ManagedIdentityWalletConfig;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.ssi.spi.IdentityWalletApiService;

@Provides({IdentityWalletApiService.class, VerifiableCredentialRegistry.class})
public class ManagedIdentityWalletExtension implements ServiceExtension {

    @Override
    public String name() {
        return "Managed Identity Wallets";
    }

    @Inject
    WebService webService;
    @Inject
    private OkHttpClient okHttpClient;

    private static final String LOG_PREFIX_SETTING = "ssi.miw.logprefix";

    /**
     * Keycloak Settings
     */
    private final String KEYCLOAK_CLIENT_ID = "ssi.miw.keycloak.clientid";
    private final String KEYCLOAK_CLIENT_SECRET = "ssi.miw.keycloak.clientsecret";
    private final String KEYCLOAK_CLIENT_GRAND_TYPE = "ssi.miw.keycloak.grandtype";
    private final String KEYCLOAK_SCOPE = "ssi.miw.keycloak.scope";

    /**
     * Access Token Settings
     */
    private final String ACCESSTOKEN_URL = "ssi.miw.accesstoken.url";

    /**
     * Connection Settings
     */
    private final String WALLET_URL = "ssi.miw.url";
    private final String WALLET_DID = "ssi.miw.did";
    private final String WALLET_JWKS_URL = "ssi.miw.url";
    private final String WALLET_ISSUER_URL = "ssi.miw.url";

    private final String OWNER_BPN = "ssi.miw.bpn";
    @Inject
    DataManagementApiConfiguration config;

    private ManagedIdentityWalletConfig walletConfig;
    @Override
    public void initialize(ServiceExtensionContext context) {
        var logPrefix = context.getSetting(LOG_PREFIX_SETTING, "MIW");
        var typeManager = context.getTypeManager();

        ManagedIdentityWalletConfig.Builder walletBuilderConfig = ManagedIdentityWalletConfig.Builder.newInstance();
        this.walletConfig = walletBuilderConfig.walletURL(context.getConfig().getString(WALLET_URL))
                .walletDID(context.getConfig().getString(WALLET_DID))
                .walletJwksURL(context.getConfig().getString(WALLET_JWKS_URL))
                .walletIssuerURL(context.getConfig().getString(WALLET_ISSUER_URL))
                .accessTokenURL(context.getConfig().getString(ACCESSTOKEN_URL))
                .keycloakClientID(context.getConfig().getString(KEYCLOAK_CLIENT_ID))
                .keycloakClientSecret(context.getConfig().getString(KEYCLOAK_CLIENT_SECRET))
                .keycloakGrandType(context.getConfig().getString(KEYCLOAK_CLIENT_GRAND_TYPE))
                .keycloakScope(context.getConfig().getString(KEYCLOAK_SCOPE))
                .ownerBPN(context.getConfig().getString(OWNER_BPN))
                .build();

        VerifiableCredentialRegistry credentialRegistry = new VerifiableCredentialRegistryImpl();
        context.registerService(VerifiableCredentialRegistry.class, credentialRegistry);
        context.registerService(IdentityWalletApiService.class,
                new ManagedIdentityWalletApiServiceImpl(context.getMonitor(), logPrefix, walletConfig, okHttpClient, typeManager, credentialRegistry));
    }
}
