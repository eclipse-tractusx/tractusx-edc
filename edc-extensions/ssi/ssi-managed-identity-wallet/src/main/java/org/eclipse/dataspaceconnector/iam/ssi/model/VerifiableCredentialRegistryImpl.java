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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

public final class VerifiableCredentialRegistryImpl implements VerifiableCredentialRegistry{

  private static final VerifiableCredentialRegistry instance = new VerifiableCredentialRegistryImpl();

  public static VerifiableCredentialRegistry getInstance() { return instance; }

  private final Map<String, VerifiableCredentialDto> verifiableCredentialMap;

  public VerifiableCredentialRegistryImpl() {
    verifiableCredentialMap = new ConcurrentHashMap<>();
  }

  @Override
  public void addVerifableCredential(VerifiableCredentialDto vc) {
    verifiableCredentialMap.put(vc.getType().get(0).toString(), vc);
  }

  public void clearRegistry(){
    this.verifiableCredentialMap.clear();
  }

  @Override
  public VerifiableCredentialDto getVerifiableCredential(String name) throws Exception {
    if(verifiableCredentialMap.containsKey(name)){
      return verifiableCredentialMap.get(name);
    } else {
      throw new Exception(format("Credential with type %s not found", name));
    }
  }
}
