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

package org.eclipse.dataspaceconnector.iam.ssi.core;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.dataspaceconnector.iam.ssi.core.claims.SSIVerifiableCredentials;
import org.eclipse.dataspaceconnector.iam.ssi.core.claims.SSIVerifiableCredentialsImpl;
import org.eclipse.dataspaceconnector.iam.ssi.core.claims.SSIVerifiablePresentation;
import org.eclipse.dataspaceconnector.iam.ssi.core.claims.SSIVerifiablePresentationImpl;
import org.eclipse.dataspaceconnector.iam.ssi.model.VerifiableCredentialRegistry;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.ssi.spi.IdentityWalletApiService;

@Provides({IdentityService.class, SSIVerifiableCredentials.class, SSIVerifiablePresentation.class})
public class SSIIdentityServiceExtension implements ServiceExtension {

  @Override
  public String name() {
    return "SSI Identity Service";
  }

  @Inject
  WebService webService;

  @Inject
  private OkHttpClient okHttpClient;

  @Inject
  public static IdentityWalletApiService walletApiService;

  public SSIVerifiablePresentation verifiablePresentation;

  public SSIVerifiableCredentials verifiableCredentials;


  @Inject
  public VerifiableCredentialRegistry verifiableCredentialRegistry;

  private static final String LOG_PREFIX_SETTING = "ssi.miw.logprefix";

  @Inject
  DataManagementApiConfiguration config;


  @Override
  public void initialize(ServiceExtensionContext context) {
    var logPrefix = context.getSetting(LOG_PREFIX_SETTING, "MIW");
    var typeManager = context.getTypeManager();
    context.getMonitor().debug("Starting initializing of SSI Identity Service");

    verifiableCredentials = new SSIVerifiableCredentialsImpl(walletApiService, verifiableCredentialRegistry);
    verifiablePresentation = new SSIVerifiablePresentationImpl(walletApiService);
    context.registerService(VerifiableCredentialRegistry.class, verifiableCredentialRegistry);
    context.registerService(SSIVerifiableCredentials.class, verifiableCredentials);
    context.registerService(SSIVerifiablePresentation.class, verifiablePresentation);
    context.registerService(IdentityService.class,
            new SSIIdentityServiceImpl(walletApiService, verifiableCredentials, verifiablePresentation));
  }
}
