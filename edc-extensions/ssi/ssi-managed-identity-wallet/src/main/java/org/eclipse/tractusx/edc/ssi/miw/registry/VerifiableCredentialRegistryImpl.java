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
package org.eclipse.tractusx.edc.ssi.miw.registry;

import static java.lang.String.format;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.tractusx.edc.ssi.miw.model.VerifiableCredentialDto;

public final class VerifiableCredentialRegistryImpl implements VerifiableCredentialRegistry {

  private static final VerifiableCredentialRegistry instance =
      new VerifiableCredentialRegistryImpl();

  public static VerifiableCredentialRegistry getInstance() {
    return instance;
  }

  private final Map<String, VerifiableCredentialDto> verifiableCredentialMap;

  public VerifiableCredentialRegistryImpl() {
    verifiableCredentialMap = new ConcurrentHashMap<>();
  }

  @Override
  public void addVerifiableCredential(VerifiableCredentialDto vc) {
    verifiableCredentialMap.put(vc.getType().get(0).toString(), vc);
  }

  @Override
  public void clearRegistry() {
    this.verifiableCredentialMap.clear();
  }

  @Override
  public VerifiableCredentialDto getVerifiableCredential(String name) throws Exception {
    if (verifiableCredentialMap.containsKey(name)) {
      return verifiableCredentialMap.get(name);
    }
    throw new Exception(format("Credential with type %s not found", name));
  }
}
