/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Add vault health check
 *
 */

package net.catenax.edc.hashicorpvault;

import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Builder
@Getter
public class HashicorpVaultHealthResponse {

  @Nullable private HashicorpVaultHealthResponsePayload payload;

  private int code;

  public HashiCorpVaultHealthResponseCode getCodeAsEnum() {
    switch (code) {
      case 200:
        return HashicorpVaultHealthResponse.HashiCorpVaultHealthResponseCode
            .INITIALIZED_UNSEALED_AND_ACTIVE;
      case 409:
        return HashicorpVaultHealthResponse.HashiCorpVaultHealthResponseCode.UNSEALED_AND_STANDBY;
      case 472:
        return HashicorpVaultHealthResponse.HashiCorpVaultHealthResponseCode
            .DISASTER_RECOVERY_MODE_REPLICATION_SECONDARY_AND_ACTIVE;
      case 473:
        return HashicorpVaultHealthResponse.HashiCorpVaultHealthResponseCode.PERFORMANCE_STANDBY;
      case 501:
        return HashicorpVaultHealthResponse.HashiCorpVaultHealthResponseCode.NOT_INITIALIZED;
      case 503:
        return HashicorpVaultHealthResponse.HashiCorpVaultHealthResponseCode.SEALED;
      default:
        return HashicorpVaultHealthResponse.HashiCorpVaultHealthResponseCode.UNSPECIFIED;
    }
  }

  public enum HashiCorpVaultHealthResponseCode {
    UNSPECIFIED, // undefined status codes
    INITIALIZED_UNSEALED_AND_ACTIVE, // status code 200
    UNSEALED_AND_STANDBY, // status code 429
    DISASTER_RECOVERY_MODE_REPLICATION_SECONDARY_AND_ACTIVE, // status code 472
    PERFORMANCE_STANDBY, // status code 473
    NOT_INITIALIZED, // status code 501
    SEALED // status code 503
  }
}
