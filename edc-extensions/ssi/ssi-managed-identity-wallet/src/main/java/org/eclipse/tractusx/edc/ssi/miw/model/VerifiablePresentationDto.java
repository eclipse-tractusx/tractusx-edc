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

public class VerifiablePresentationDto {

  @JsonProperty("@context")
  private final List<String> context;

  private final String id;

  private final List<String> type;

  private final String holder;

  @JsonProperty("verifiableCredential")
  private final List<VerifiableCredentialDto> verifiableCredentials;

  private final LdProofDto proof;

  public VerifiablePresentationDto(
      @JsonProperty("@context") List<String> context,
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
