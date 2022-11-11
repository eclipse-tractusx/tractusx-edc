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
import java.util.List;

public class DidDocumentDto {

  private final String id;

  private final List<String> context;

  private final List<DidVerificationMethodDto> verificationMethodDtos;

  private final List<Object> authenticationVerificationMethods;

  private final List<Object> assertionMethodVerificationMethods;

  private final List<Object> keyAgreementVerificationMethods;

  private final List<Object> capabilityInvocationVerificationMethods;

  private final List<Object> capabilityDelegationVerificationMethods;

  private final List<DidServiceDto> services;

  public DidDocumentDto(
      @JsonProperty("id") String id,
      @JsonProperty("@context") List<String> context,
      @JsonProperty("verificationMethod") List<DidVerificationMethodDto> verificationMethodDtos,
      @JsonProperty("authentication") List<Object> authenticationVerificationMethods,
      @JsonProperty("assertionMethod") List<Object> assertionMethodVerificationMethods,
      @JsonProperty("keyAgreement") List<Object> keyAgreementVerificationMethods,
      @JsonProperty("capabilityInvocation") List<Object> capabilityInvocationVerificationMethods,
      @JsonProperty("capabilityDelegation") List<Object> capabilityDelegationVerificationMethods,
      @JsonProperty("service") List<DidServiceDto> services) {
    this.id = id;
    this.context = context;
    this.verificationMethodDtos = verificationMethodDtos;
    this.authenticationVerificationMethods = authenticationVerificationMethods;
    this.assertionMethodVerificationMethods = assertionMethodVerificationMethods;
    this.keyAgreementVerificationMethods = keyAgreementVerificationMethods;
    this.capabilityInvocationVerificationMethods = capabilityInvocationVerificationMethods;
    this.capabilityDelegationVerificationMethods = capabilityDelegationVerificationMethods;
    this.services = services;
  }

  public String getId() {
    return id;
  }

  public List<String> getContext() {
    return context;
  }

  public List<DidVerificationMethodDto> getVerificationMethodDtos() {
    return verificationMethodDtos;
  }

  public List<Object> getAuthenticationVerificationMethods() {
    return authenticationVerificationMethods;
  }

  public List<Object> getAssertionMethodVerificationMethods() {
    return assertionMethodVerificationMethods;
  }

  public List<Object> getKeyAgreementVerificationMethods() {
    return keyAgreementVerificationMethods;
  }

  public List<Object> getCapabilityInvocationVerificationMethods() {
    return capabilityInvocationVerificationMethods;
  }

  public List<Object> getCapabilityDelegationVerificationMethods() {
    return capabilityDelegationVerificationMethods;
  }

  public List<DidServiceDto> getServices() {
    return services;
  }
}
