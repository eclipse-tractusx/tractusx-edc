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

public class AccessTokenDescriptionDto {

  private final String tokenID;

  private final String accessToken;

  private final String expiresIn;

  private final String refreshExpiresIn;

  private final String tokenType;
  private final String notBeforePolicy;
  private final String scope;

  @JsonCreator
  public AccessTokenDescriptionDto(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("expires_in") String expiresIn,
      @JsonProperty("id_token") String tokenID,
      @JsonProperty("not-before-policy") String notBeforePolicy,
      @JsonProperty("refresh_expires_in") String refreshExpiresIn,
      @JsonProperty("scope") String scope,
      @JsonProperty("token_type") String tokenType) {
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
