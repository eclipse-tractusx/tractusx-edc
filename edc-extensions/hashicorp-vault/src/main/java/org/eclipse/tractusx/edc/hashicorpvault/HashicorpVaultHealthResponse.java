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

package org.eclipse.tractusx.edc.hashicorpvault;

public class HashicorpVaultHealthResponse {

    private HashicorpVaultHealthResponsePayload payload;
    private int code;

    private HashicorpVaultHealthResponse() {
    }

    public int getCode() {
        return code;
    }

    public HashiCorpVaultHealthResponseCode getCodeAsEnum() {
        switch (code) {
            case 200:
                return HashicorpVaultHealthResponse.HashiCorpVaultHealthResponseCode
                        .INITIALIZED_UNSEALED_AND_ACTIVE;
            case 429:
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

    public HashicorpVaultHealthResponsePayload getPayload() {
        return payload;
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

    public static final class Builder {

        private final HashicorpVaultHealthResponse response;

        private Builder() {
            response = new HashicorpVaultHealthResponse();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder payload(HashicorpVaultHealthResponsePayload payload) {
            this.response.payload = payload;
            return this;
        }

        public Builder code(int code) {
            this.response.code = code;
            return this;
        }

        public HashicorpVaultHealthResponse build() {
            return response;
        }
    }
}
