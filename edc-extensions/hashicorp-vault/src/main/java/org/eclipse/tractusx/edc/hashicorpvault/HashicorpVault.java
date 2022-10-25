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
 *
 */

package org.eclipse.tractusx.edc.hashicorpvault;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Implements a vault backed by Hashicorp Vault. */
@RequiredArgsConstructor
class HashicorpVault implements Vault {

  @NonNull private final HashicorpVaultClient hashicorpVaultClient;
  @NonNull private final Monitor monitor;

  @Override
  public @Nullable String resolveSecret(@NonNull String key) {
    Result<String> result = hashicorpVaultClient.getSecretValue(key);

    return result.succeeded() ? result.getContent() : null;
  }

  @Override
  @NotNull
  public Result<Void> storeSecret(@NotNull @NonNull String key, @NotNull @NonNull String value) {
    Result<HashicorpVaultCreateEntryResponsePayload> result =
        hashicorpVaultClient.setSecret(key, value);

    return result.succeeded() ? Result.success() : Result.failure(result.getFailureMessages());
  }

  @Override
  public Result<Void> deleteSecret(@NotNull @NonNull String key) {
    return hashicorpVaultClient.destroySecret(key);
  }
}
