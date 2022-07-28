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
 *       Mercedes-Benz Tech Innovation GmbH - Add vault health check
 *
 */

package net.catenax.edc.hashicorpvault;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;

@Requires(HealthCheckService.class)
public class HashicorpVaultHealthExtension extends AbstractHashicorpVaultExtension
    implements ServiceExtension {

  @EdcSetting
  public static final String VAULT_HEALTH_CHECK = "edc.vault.hashicorp.health.check.enabled";

  public static final boolean VAULT_HEALTH_CHECK_DEFAULT = true;

  @Override
  public String name() {
    return "Hashicorp Vault Health Check";
  }

  @Override
  public void initialize(ServiceExtensionContext context) {
    final HashicorpVaultClientConfig config = loadHashicorpVaultClientConfig(context);

    final OkHttpClient okHttpClient = createOkHttpClient(config);

    final HashicorpVaultClient client =
        new HashicorpVaultClient(config, okHttpClient, context.getTypeManager().getMapper());

    configureHealthCheck(client, context);

    context.getMonitor().info("HashicorpVaultExtension: health check initialization complete.");
  }

  private void configureHealthCheck(HashicorpVaultClient client, ServiceExtensionContext context) {
    final boolean healthCheckEnabled =
        context.getSetting(VAULT_HEALTH_CHECK, VAULT_HEALTH_CHECK_DEFAULT);
    if (!healthCheckEnabled) return;

    final HashicorpVaultHealthCheck healthCheck =
        new HashicorpVaultHealthCheck(client, context.getMonitor());

    final HealthCheckService healthCheckService = context.getService(HealthCheckService.class);
    healthCheckService.addLivenessProvider(healthCheck);
    healthCheckService.addReadinessProvider(healthCheck);
    healthCheckService.addStartupStatusProvider(healthCheck);
  }
}
