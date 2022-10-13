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

import java.util.List;

public class VerifiablePresentationRequestDto {

  private final String holderIdentifier;

  private final List<VerifiableCredentialDto> verifiableCredentials;


  public VerifiablePresentationRequestDto(String holderIdentifier,
                                          List<VerifiableCredentialDto> verifiableCredentials) {
    this.holderIdentifier = holderIdentifier;
    this.verifiableCredentials = verifiableCredentials;
  }

  public String getHolderIdentifier() {
    return holderIdentifier;
  }

  public List<VerifiableCredentialDto> getVerifiableCredentials() {
    return verifiableCredentials;
  }
}
