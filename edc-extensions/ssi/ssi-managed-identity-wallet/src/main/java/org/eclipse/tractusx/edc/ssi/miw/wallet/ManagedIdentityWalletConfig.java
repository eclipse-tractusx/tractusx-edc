/*
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
package org.eclipse.tractusx.edc.ssi.miw.wallet;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/** Config Class for the endpoints and user management of the Managed Identity Wallet */
@JsonDeserialize(builder = ManagedIdentityWalletConfig.Builder.class)
public class ManagedIdentityWalletConfig {

  @NotNull private String walletURL;
  @NotNull private String walletDID;
  @NotNull private String keycloakClientID;
  @NotNull private String keycloakClientSecret;
  @NotNull private String keycloakGrandType;
  @NotNull private String keycloakScope;
  @NotNull private String accessTokenURL;
  @NotNull private String ownerBPN;
  @NotNull private List<String> didsOfTrustedProviders;

  @NotNull private String logPrefix;

  private ManagedIdentityWalletConfig() {}

  public String getWalletURL() {
    return walletURL;
  }

  public String getWalletDID() {
    return walletDID;
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

  public String getLogPrefix() {
    return logPrefix;
  }

  public String getOwnerBPN() {
    return ownerBPN;
  }

  public List<String> getDidOfTrustedProviders() {
    return didsOfTrustedProviders;
  }

  @Override
  public String toString() {
    return "ManagedIdentityWalletConfig{"
        + "walletURL='"
        + walletURL
        + '\''
        + ", walletDID='"
        + walletDID
        + '\''
        + ", keycloakClientID='"
        + keycloakClientID
        + '\''
        + ", keycloakClientSecret='"
        + keycloakClientSecret
        + '\''
        + ", keycloakGrandType='"
        + keycloakGrandType
        + '\''
        + ", keycloakScope='"
        + keycloakScope
        + '\''
        + ", accessTokenURL='"
        + accessTokenURL
        + '\''
        + ", ownerBPN='"
        + ownerBPN
        + '\''
        + ", trusted providers='"
        + didsOfTrustedProviders
        + '\''
        + ", logPrefix='"
        + logPrefix
        + '\''
        + '}';
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class Builder {
    private final ManagedIdentityWalletConfig walletConfig;

    public static Builder newInstance() {
      return new Builder();
    }

    private Builder() {
      walletConfig = new ManagedIdentityWalletConfig();
    }

    public Builder accessTokenURL(String accessTokenURL) {
      walletConfig.accessTokenURL = accessTokenURL;
      return this;
    }

    public Builder walletURL(String walletURL) {
      walletConfig.walletURL = walletURL;
      return this;
    }

    public Builder walletDID(String walletDID) {
      walletConfig.walletDID = walletDID;
      return this;
    }

    public Builder keycloakClientID(String keycloakClientID) {
      walletConfig.keycloakClientID = keycloakClientID;
      return this;
    }

    public Builder keycloakClientSecret(String keycloakClientSecret) {
      walletConfig.keycloakClientSecret = keycloakClientSecret;
      return this;
    }

    public Builder keycloakGrandType(String keycloakGrandType) {
      walletConfig.keycloakGrandType = keycloakGrandType;
      return this;
    }

    public Builder ownerBPN(String ownerBPN) {
      walletConfig.ownerBPN = ownerBPN;
      return this;
    }

    public Builder keycloakScope(String keycloakScope) {
      walletConfig.keycloakScope = keycloakScope;
      return this;
    }

    public Builder trustedProvider(List<String> didsOfTrustedProviders) {
      walletConfig.didsOfTrustedProviders = didsOfTrustedProviders;
      return this;
    }

    public ManagedIdentityWalletConfig build() {
      return walletConfig;
    }
  }
}
