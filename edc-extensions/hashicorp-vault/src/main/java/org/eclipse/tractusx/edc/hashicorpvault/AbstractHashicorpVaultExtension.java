/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
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

package org.eclipse.tractusx.edc.hashicorpvault;

import java.time.Duration;
import okhttp3.OkHttpClient;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

/**
 * Temporary solution as long as the Vault components needs to be loaded as dedicated vault
 * extension. Will be changed from EDC milestone 5.
 */
public class AbstractHashicorpVaultExtension {

  public static final String VAULT_URL = "edc.vault.hashicorp.url";

  public static final String VAULT_TOKEN = "edc.vault.hashicorp.token";

  public static final String VAULT_API_SECRET_PATH = "edc.vault.hashicorp.api.secret.path";

  public static final String VAULT_API_SECRET_PATH_DEFAULT = "/v1/secret";

  public static final String VAULT_API_HEALTH_PATH = "edc.vault.hashicorp.api.health.check.path";

  public static final String VAULT_API_HEALTH_PATH_DEFAULT = "/v1/sys/health";

  public static final String VAULT_HEALTH_CHECK_STANDBY_OK =
      "edc.vault.hashicorp.health.check.standby.ok";

  public static final boolean VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT = false;

  private static final String VAULT_TIMEOUT_SECONDS = "edc.vault.hashicorp.timeout.seconds";

  protected OkHttpClient createOkHttpClient(HashicorpVaultClientConfig config) {
    OkHttpClient.Builder builder =
        new OkHttpClient.Builder()
            .callTimeout(config.getTimeout())
            .readTimeout(config.getTimeout());

    return builder.build();
  }

  protected HashicorpVaultClientConfig loadHashicorpVaultClientConfig(
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
}
