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

  public DidDocumentDto(@JsonProperty("id") String id,
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
