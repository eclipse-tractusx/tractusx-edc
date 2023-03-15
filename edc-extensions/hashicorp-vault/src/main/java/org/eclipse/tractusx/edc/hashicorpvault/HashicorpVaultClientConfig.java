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

import java.time.Duration;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Builder
@Getter
@RequiredArgsConstructor
class HashicorpVaultClientConfig {
  @NonNull private final String vaultUrl;
  @NonNull private final String vaultToken;
  @NonNull private final String vaultApiSecretPath;
  @NonNull private final String vaultApiHealthPath;
  @NonNull private final Duration timeout;

  private final boolean isVaultApiHealthStandbyOk;
}
