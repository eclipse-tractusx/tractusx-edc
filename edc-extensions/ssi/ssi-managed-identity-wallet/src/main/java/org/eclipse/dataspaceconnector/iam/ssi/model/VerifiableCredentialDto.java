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
import java.util.Map;

public class VerifiableCredentialDto {

  private final String id;

  @JsonProperty("@context")
  private final List<String> context;

  private final List<String> type;

  private final String issuer;

  private final String issuanceDate;

  private final String expirationDate;

  private final Map<String, Object> credentialSubject;

  private final LdProofDto proof;

  public VerifiableCredentialDto(@JsonProperty("id") String id,
                                 @JsonProperty("@context") List<String> context,
                                 @JsonProperty("type") List<String> type,
                                 @JsonProperty("issuer") String issuer,
                                 @JsonProperty("issuanceDate") String issuanceDate,
                                 @JsonProperty("expirationDate") String expirationDate,
                                 @JsonProperty("credentialSubject") Map<String, Object> credentialSubject,
                                 @JsonProperty("proof") LdProofDto proof) {
    this.id = id;
    this.context = context;
    this.type = type;
    this.issuer = issuer;
    this.issuanceDate = issuanceDate;
    this.expirationDate = expirationDate;
    this.credentialSubject = credentialSubject;
    this.proof = proof;
  }

  public String getId() {
    return id;
  }

  public List<String> getContext() {
    return context;
  }

  public List<String> getType() {
    return type;
  }

  public String getIssuer() {
    return issuer;
  }

  public String getIssuanceDate() {
    return issuanceDate;
  }

  public String getExpirationDate() {
    return expirationDate;
  }

  public Map<String, Object> getCredentialSubject() {
    return credentialSubject;
  }

  public LdProofDto getProof() {
    return proof;
  }
}
