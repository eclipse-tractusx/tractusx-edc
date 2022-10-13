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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AccessTokenDescriptionDto {

    private final String tokenID;

    private final String accessToken;

    private final String expiresIn;

    private final String refreshExpiresIn;

    private final String tokenType;
    private final String notBeforePolicy;
    private final String scope;

    @JsonCreator
    public AccessTokenDescriptionDto(@JsonProperty("access_token") String accessToken,
                                     @JsonProperty("expires_in") String expiresIn,
                                     @JsonProperty("id_token") String tokenID,
                                     @JsonProperty("not-before-policy") String notBeforePolicy,
                                     @JsonProperty("refresh_expires_in") String refreshExpiresIn,
                                     @JsonProperty("scope") String scope,
                                     @JsonProperty("token_type") String tokenType){
            this.tokenID = tokenID;
            this.accessToken = accessToken;
            this.expiresIn = expiresIn;
            this.refreshExpiresIn = refreshExpiresIn;
            this.tokenType = tokenType;
            this.notBeforePolicy = notBeforePolicy;
            this.scope = scope;
    }

    public String getTokenID() {
        return tokenID;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getExpiresIn() {
        return expiresIn;
    }

    public String getRefreshExpiresIn() {
        return refreshExpiresIn;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getNotBeforePolicy() {
        return notBeforePolicy;
    }

    public String getScope() {
        return scope;
    }
}
