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
package org.eclipse.dataspaceconnector.iam.ssi.wallet;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Config Class for the endpoints and user management of the Managed Identity Wallet
 */
@JsonDeserialize(builder = ManagedIdentityWalletConfig.Builder.class)
public class ManagedIdentityWalletConfig {

    @NotNull
    private String walletURL;
    @NotNull
    private String walletDID;
    @NotNull
    private String walletJwksURL;
    @NotNull
    private String walletIssuerURL;
    @NotNull
    private String keycloakClientID;
    @NotNull
    private String keycloakClientSecret;
    @NotNull
    private String keycloakGrandType;
    @NotNull
    private String keycloakScope;
    @NotNull
    private String accessTokenURL;
    @NotNull
    private String ownerBPN;

    @NotNull
    private String logprefig;

    @Override
    public String toString() {
        return "ManagedIdentityWalletConfig{" +
                "walletURL='" + walletURL + '\'' +
                ", walletDID='" + walletDID + '\'' +
                ", walletJwksURL='" + walletJwksURL + '\'' +
                ", walletIssuerURL='" + walletIssuerURL + '\'' +
                ", keycloakClientID='" + keycloakClientID + '\'' +
                ", keycloakClientSecret='" + keycloakClientSecret + '\'' +
                ", keycloakGrandType='" + keycloakGrandType + '\'' +
                ", keycloakScope='" + keycloakScope + '\'' +
                ", accessTokenURL='" + accessTokenURL + '\'' +
                ", ownerBPN='" + ownerBPN + '\'' +
                ", logprefig='" + logprefig + '\'' +
                '}';
    }

    private ManagedIdentityWalletConfig(){}

    public String getWalletURL() {
        return walletURL;
    }

    public String getWalletDID() { return walletDID; }

    public String getWalletJwksURL() {
        return walletJwksURL;
    }

    public String getWalletIssuerURL() {
        return walletIssuerURL;
    }

    public String getKeycloakClientID() {
        return keycloakClientID;
    }

    public String getKeycloakClientSecret() {
        return keycloakClientSecret;
    }

    public String getKeycloakGrandType() {
        return keycloakGrandType;
    }

    public String getKeycloakScope() {
        return keycloakScope;
    }

    public String getAccessTokenURL() {
        return accessTokenURL;
    }

    public String getLogprefig() {
        return logprefig;
    }

    public String getOwnerBPN() {
        return ownerBPN;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder{
        private final ManagedIdentityWalletConfig walletConfig;

        public static Builder newInstance(){
            return new Builder();
        }

        private Builder(){
            walletConfig = new ManagedIdentityWalletConfig();
        }

        public Builder accessTokenURL(String accessTokenURL){
            walletConfig.accessTokenURL = accessTokenURL;
            return this;
        }

        public Builder walletURL(String walletURL){
            walletConfig.walletURL = walletURL;
            return this;
        }

        public Builder walletDID(String walletDID){
            walletConfig.walletDID = walletDID;
            return this;
        }

        public Builder walletJwksURL(String walletJwksURL){
            walletConfig.walletJwksURL = walletJwksURL;
            return this;
        }

        public Builder walletIssuerURL(String walletIssuerURL){
            walletConfig.walletIssuerURL = walletIssuerURL;
            return this;
        }

        public Builder keycloakClientID(String keycloakClientID){
            walletConfig.keycloakClientID = keycloakClientID;
            return this;
        }

        public Builder keycloakClientSecret(String keycloakClientSecret){
            walletConfig.keycloakClientSecret = keycloakClientSecret;
            return this;
        }

        public Builder keycloakGrandType(String keycloakGrandType){
            walletConfig.keycloakGrandType = keycloakGrandType;
            return this;
        }

        public Builder ownerBPN(String ownerBPN){
            walletConfig.ownerBPN = ownerBPN;
            return this;
        }

        public Builder keycloakScope(String keycloakScope){
            walletConfig.keycloakScope = keycloakScope;
            return this;
        }

        public ManagedIdentityWalletConfig build(){
            return walletConfig;
        }
    }


}