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

package org.eclipse.dataspaceconnector.iam.ssi.core.did;

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

  public DidVerificationMethodDto(@JsonProperty("id") String id,
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
