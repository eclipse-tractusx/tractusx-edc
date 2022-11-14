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
package org.eclipse.tractusx.edc.ssi.miw.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

public class AccessTokenRequestDto {

  private String clientId;

  private String clientSecret;

  private String grantType;

  private String scope;

  public AccessTokenRequestDto() {}

  @JsonProperty("client_id")
  public String getClientId() {
    return clientId;
  }

  @JsonProperty("client_secret")
  public String getClientSecret() {
    return clientSecret;
  }

  @JsonProperty("grant_type")
  public String getGrantType() {
    return grantType;
  }

  @JsonProperty("scope")
  public String getScope() {
    return scope;
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static final class Builder {
    private final AccessTokenRequestDto dto;

    private Builder() {
      dto = new AccessTokenRequestDto();
    }

    @JsonCreator
    public static Builder newInstance() {
      return new Builder();
    }

    public Builder clientID(String clientID) {
      dto.clientId = clientID;
      return this;
    }

    public Builder clientSecret(String clientSecret) {
      dto.clientSecret = clientSecret;
      return this;
    }

    public Builder grandType(String grandType) {
      dto.grantType = grandType;
      return this;
    }

    public Builder scope(String scope) {
      dto.scope = scope;
      return this;
    }

    public AccessTokenRequestDto build() {
      return dto;
    }
  }
}
