/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *       Mercedes-Benz Tech Innovation GmbH - Make secret data & metadata paths configurable
 *       Mercedes-Benz Tech Innovation GmbH - Add vault health check
 *
 */

package net.catenax.edc.hashicorpvault;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.security.VaultPrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Provides({Vault.class, CertificateResolver.class, PrivateKeyResolver.class})
public class HashicorpVaultVaultExtension extends AbstractHashicorpVaultExtension
    implements ServiceExtension {

  @Override
  public String name() {
    return "Hashicorp Vault";
  }

  @Override
  public void initialize(ServiceExtensionContext context) {
    final HashicorpVaultClientConfig config = loadHashicorpVaultClientConfig(context);

    final OkHttpClient okHttpClient = createOkHttpClient(config);

    final HashicorpVaultClient client =
        new HashicorpVaultClient(config, okHttpClient, context.getTypeManager().getMapper());

    final HashicorpVault vault = new HashicorpVault(client, context.getMonitor());
    final CertificateResolver certificateResolver =
        new HashicorpCertificateResolver(vault, context.getMonitor());
    final VaultPrivateKeyResolver privateKeyResolver = new VaultPrivateKeyResolver(vault);

    context.registerService(Vault.class, vault);
    context.registerService(CertificateResolver.class, certificateResolver);
    context.registerService(PrivateKeyResolver.class, privateKeyResolver);

    context.getMonitor().info("HashicorpVaultExtension: authentication/initialization complete.");
  }
}
