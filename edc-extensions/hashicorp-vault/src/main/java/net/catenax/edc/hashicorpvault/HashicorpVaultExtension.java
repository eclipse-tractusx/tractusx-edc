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

import java.time.Duration;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.security.VaultPrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.VaultExtension;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;

@Requires(HealthCheckService.class)
public class HashicorpVaultExtension implements VaultExtension {

  @EdcSetting(required = true)
  public static final String VAULT_URL = "edc.vault.hashicorp.url";

  @EdcSetting(required = true)
  public static final String VAULT_TOKEN = "edc.vault.hashicorp.token";

  @EdcSetting
  public static final String VAULT_API_SECRET_PATH = "edc.vault.hashicorp.api.secret.path";

  public static final String VAULT_API_SECRET_PATH_DEFAULT = "/v1/secret";

  @EdcSetting
  public static final String VAULT_API_HEALTH_PATH = "edc.vault.hashicorp.api.health.check.path";

  public static final String VAULT_API_HEALTH_PATH_DEFAULT = "/sys/health";

  @EdcSetting
  public static final String VAULT_HEALTH_CHECK = "edc.vault.hashicorp.health.check.enabled";

  public static final boolean VAULT_HEALTH_CHECK_DEFAULT = true;

  @EdcSetting
  public static final String VAULT_HEALTH_CHECK_STANDBY_OK =
      "edc.vault.hashicorp.health.check.standby.ok";

  public static final boolean VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT = false;

  @EdcSetting
  private static final String VAULT_TIMEOUT_SECONDS = "edc.vault.hashicorp.timeout.seconds";

  private Vault vault;
  private CertificateResolver certificateResolver;
  private PrivateKeyResolver privateKeyResolver;

  @Override
  public String name() {
    return "Hashicorp Vault";
  }

  @Override
  public Vault getVault() {
    return vault;
  }

  @Override
  public PrivateKeyResolver getPrivateKeyResolver() {
    return privateKeyResolver;
  }

  @Override
  public CertificateResolver getCertificateResolver() {
    return certificateResolver;
  }

  @Override
  public void initializeVault(ServiceExtensionContext context) {
    final HashicorpVaultClientConfig config = loadHashicorpVaultClientConfig(context);

    final OkHttpClient okHttpClient = createOkHttpClient(config);

    final HashicorpVaultClient client =
        new HashicorpVaultClient(config, okHttpClient, context.getTypeManager().getMapper());

    vault = new HashicorpVault(client, context.getMonitor());
    certificateResolver = new HashicorpCertificateResolver(vault, context.getMonitor());
    privateKeyResolver = new VaultPrivateKeyResolver(vault);

    configureHealthCheck(client, context);

    context.getMonitor().info("HashicorpVaultExtension: authentication/initialization complete.");
  }

  private OkHttpClient createOkHttpClient(HashicorpVaultClientConfig config) {
    OkHttpClient.Builder builder =
        new OkHttpClient.Builder()
            .callTimeout(config.getTimeout())
            .readTimeout(config.getTimeout());

    return builder.build();
  }

  private HashicorpVaultClientConfig loadHashicorpVaultClientConfig(
      ServiceExtensionContext context) {

    final String vaultUrl = context.getSetting(VAULT_URL, null);
    if (vaultUrl == null) {
      throw new HashicorpVaultException(String.format("Vault URL (%s) must be defined", VAULT_URL));
    }

    final int vaultTimeoutSeconds = Math.max(0, context.getSetting(VAULT_TIMEOUT_SECONDS, 30));
    final Duration vaultTimeoutDuration = Duration.ofSeconds(vaultTimeoutSeconds);

    final String vaultToken = context.getSetting(VAULT_TOKEN, null);

    if (vaultToken == null) {
      throw new HashicorpVaultException(
          String.format("For Vault authentication [%s] is required", VAULT_TOKEN));
    }

    final String apiSecretPath =
        context.getSetting(VAULT_API_SECRET_PATH, VAULT_API_SECRET_PATH_DEFAULT);

    final String apiHealthPath =
        context.getSetting(VAULT_API_HEALTH_PATH, VAULT_API_HEALTH_PATH_DEFAULT);

    final boolean isHealthStandbyOk =
        context.getSetting(VAULT_HEALTH_CHECK_STANDBY_OK, VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT);

    return HashicorpVaultClientConfig.builder()
        .vaultUrl(vaultUrl)
        .vaultToken(vaultToken)
        .vaultApiSecretPath(apiSecretPath)
        .vaultApiHealthPath(apiHealthPath)
        .isVaultApiHealthStandbyOk(isHealthStandbyOk)
        .timeout(vaultTimeoutDuration)
        .build();
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
