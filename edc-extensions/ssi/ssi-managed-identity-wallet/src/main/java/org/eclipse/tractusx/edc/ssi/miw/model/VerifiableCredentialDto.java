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

  private final CredentialStatus credentialStatus;

  private final LdProofDto proof;

  public VerifiableCredentialDto(
      @JsonProperty("id") String id,
      @JsonProperty("@context") List<String> context,
      @JsonProperty("type") List<String> type,
      @JsonProperty("issuer") String issuer,
      @JsonProperty("issuanceDate") String issuanceDate,
      @JsonProperty("expirationDate") String expirationDate,
      @JsonProperty("credentialSubject") Map<String, Object> credentialSubject,
      @JsonProperty("credentialStatus") CredentialStatus credentialStatus,
      @JsonProperty("proof") LdProofDto proof) {
    this.id = id;
    this.context = context;
    this.type = type;
    this.issuer = issuer;
    this.issuanceDate = issuanceDate;
    this.expirationDate = expirationDate;
    this.credentialSubject = credentialSubject;
    this.credentialStatus = credentialStatus;
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

  public CredentialStatus getCredentialStatus() {
    return credentialStatus;
  }

  public LdProofDto getProof() {
    return proof;
  }
}
