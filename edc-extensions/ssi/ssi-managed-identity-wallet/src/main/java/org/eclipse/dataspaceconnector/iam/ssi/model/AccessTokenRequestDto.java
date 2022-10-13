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
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

public class AccessTokenRequestDto {
    private String cliend_id;

    private String client_secret;

    private String grant_type;

    private String scope;

    //private AccessTokenDescription accessToken;

    public AccessTokenRequestDto() {
    }

    @JsonProperty("client_id")
    public String getCliendId() {
        return cliend_id;
    }

    public String getClient_secret() {
        return client_secret;
    }

    @JsonProperty("grant_type")
    public String getGrantType() {
        return grant_type;
    }

    @JsonProperty("scope")
    public String getScope() {
        return scope;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder{
        private final AccessTokenRequestDto dto;

        private Builder(){dto = new AccessTokenRequestDto();}

        @JsonCreator
        public static Builder newInstance(){ return new Builder();}

        public Builder clientID(String clientID){
            dto.cliend_id = clientID;
            return this;
        }

        public Builder clientSecret(String clientSecret){
            dto.client_secret = clientSecret;
            return this;
        }

        public Builder grandType(String grandType){
            dto.grant_type = grandType;
            return this;
        }

        public Builder scope(String scope){
            dto.scope = scope;
            return this;
        }

        public AccessTokenRequestDto build(){return dto;}
    }
}
