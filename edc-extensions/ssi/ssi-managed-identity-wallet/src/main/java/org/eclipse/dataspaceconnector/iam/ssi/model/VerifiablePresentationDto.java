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

public class VerifiablePresentationDto {

  @JsonProperty("@context")
  private final List<String> context;

  private final String id;

  private final List<String> type;

  private final String holder;

  @JsonProperty("verifiableCredential")
  private final List<VerifiableCredentialDto> verifiableCredentials;

  private final LdProofDto proof;

  public VerifiablePresentationDto(@JsonProperty("@context") List<String> context,
                                   @JsonProperty("id") String id,
                                   @JsonProperty("type") List<String> type,
                                   @JsonProperty("holder") String holder,
                                   @JsonProperty("verifiableCredential") List<VerifiableCredentialDto> verifiableCredentials,
                                   @JsonProperty("proof") LdProofDto proof) {
    this.context = context;
    this.id = id;
    this.type = type;
    this.holder = holder;
    this.verifiableCredentials = verifiableCredentials;
    this.proof = proof;
  }

  public List<String> getContext() {
    return context;
  }

  public String getId() {
    return id;
  }

  public List<String> getType() {
    return type;
  }

  public String getHolder() {
    return holder;
  }

  public List<VerifiableCredentialDto> getVerifiableCredentials() {
    return verifiableCredentials;
  }

  public LdProofDto getProof() {
    return proof;
  }
}
