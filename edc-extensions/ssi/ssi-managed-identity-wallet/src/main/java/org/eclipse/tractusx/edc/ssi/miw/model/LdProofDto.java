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

public class LdProofDto {

  private final String type;

  private final String created;

  private final String proofPurpose;

  private final String verificationMethod;

  private final String jws;

  private final String proofValue;

  private final String creator;

  private final String domain;

  private final String challenge;

  private final String nonce;

  public LdProofDto(
      @JsonProperty("type") String type,
      @JsonProperty("created") String created,
      @JsonProperty("proofPurpose") String proofPurpose,
      @JsonProperty("verificationMethod") String verificationMethod,
      @JsonProperty("jws") String jws,
      @JsonProperty("proofValue") String proofValue,
      @JsonProperty("creator") String creator,
      @JsonProperty("domain") String domain,
      @JsonProperty("challenge") String challenge,
      @JsonProperty("nonce") String nonce) {
    this.type = type;
    this.created = created;
    this.proofPurpose = proofPurpose;
    this.verificationMethod = verificationMethod;
    this.jws = jws;
    this.proofValue = proofValue;
    this.creator = creator;
    this.domain = domain;
    this.challenge = challenge;
    this.nonce = nonce;
  }

  public String getType() {
    return type;
  }

  public String getCreated() {
    return created;
  }

  public String getProofPurpose() {
    return proofPurpose;
  }

  public String getVerificationMethod() {
    return verificationMethod;
  }

  public String getJws() {
    return jws;
  }

  public String getProofValue() {
    return proofValue;
  }

  public String getCreator() {
    return creator;
  }

  public String getDomain() {
    return domain;
  }

  public String getChallenge() {
    return challenge;
  }

  public String getNonce() {
    return nonce;
  }
}
