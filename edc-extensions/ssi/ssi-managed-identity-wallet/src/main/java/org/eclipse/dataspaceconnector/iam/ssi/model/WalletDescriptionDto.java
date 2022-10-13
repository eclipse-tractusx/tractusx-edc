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

package org.eclipse.dataspaceconnector.iam.ssi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class WalletDescriptionDto {
    private final String name;

    private final String bpn;

    private final String did;

    private final String createdAt;

    private final List<VerifiableCredentialDto> verifiableCredentials;

    public WalletDescriptionDto(@JsonProperty("name") String name,
                                @JsonProperty("bpn") String bpn,
                                @JsonProperty("did") String did,
                                @JsonProperty("createdAt") String createdAt,
                                @JsonProperty("vcs") List<VerifiableCredentialDto> verifiableCredentials) {
        this.name = name;
        this.bpn = bpn;
        this.did = did;
        this.createdAt = createdAt;
        this.verifiableCredentials = verifiableCredentials;
    }

    public String getName() {
        return name;
    }

    public String getBpn() {
        return bpn;
    }

    public String getDid() {
        return did;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public List<VerifiableCredentialDto> getVerifiableCredentials() {
        return verifiableCredentials;
    }
}
