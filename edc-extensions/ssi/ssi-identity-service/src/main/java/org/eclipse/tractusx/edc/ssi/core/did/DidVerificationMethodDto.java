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
package org.eclipse.tractusx.edc.ssi.core.did;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DidVerificationMethodDto {

  private final String id;

  private final String type;

  private final String controller;

  private final String publicKeyBase64;

  private final String publicKeyBase58;

  private final String publicKeyHex;

  private final String publicKeyMultibase;

  private final String publicKeyJwk;

  public DidVerificationMethodDto(
      @JsonProperty("id") String id,
      @JsonProperty("type") String type,
      @JsonProperty("controller") String controller,
      @JsonProperty("publicKeyBase64") String publicKeyBase64,
      @JsonProperty("publicKeyBase58") String publicKeyBase58,
      @JsonProperty("publicKeyHex") String publicKeyHex,
      @JsonProperty("publicKeyMultibase") String publicKeyMultibase,
      @JsonProperty("publicKeyJwk") String publicKeyJwk) {
    this.id = id;
    this.type = type;
    this.controller = controller;
    this.publicKeyBase64 = publicKeyBase64;
    this.publicKeyBase58 = publicKeyBase58;
    this.publicKeyHex = publicKeyHex;
    this.publicKeyMultibase = publicKeyMultibase;
    this.publicKeyJwk = publicKeyJwk;
  }

  public String getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public String getController() {
    return controller;
  }

  public String getPublicKeyBase64() {
    return publicKeyBase64;
  }

  public String getPublicKeyBase58() {
    return publicKeyBase58;
  }

  public String getPublicKeyHex() {
    return publicKeyHex;
  }

  public String getPublicKeyMultibase() {
    return publicKeyMultibase;
  }

  public String getPublicKeyJwk() {
    return publicKeyJwk;
  }
}
