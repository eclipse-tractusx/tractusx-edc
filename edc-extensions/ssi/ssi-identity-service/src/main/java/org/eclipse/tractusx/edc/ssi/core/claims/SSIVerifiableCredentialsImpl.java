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
package org.eclipse.tractusx.edc.ssi.core.claims;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.tractusx.edc.ssi.miw.model.VerifiableCredentialDto;
import org.eclipse.tractusx.edc.ssi.miw.registry.VerifiableCredentialRegistry;
import org.eclipse.tractusx.edc.ssi.spi.IdentityWalletApiService;

public class SSIVerifiableCredentialsImpl implements SSIVerifiableCredentials {

  private IdentityWalletApiService walletApiService;
  private VerifiableCredentialRegistry verifiableCredentialRegistry;

  public SSIVerifiableCredentialsImpl(
      IdentityWalletApiService walletApiService,
      VerifiableCredentialRegistry verifiableCredentialRegistry) {
    this.walletApiService = walletApiService;
    this.verifiableCredentialRegistry = verifiableCredentialRegistry;
  }

  @Override
  public VerifiableCredentialDto findByScope(String scope) {
    // Update VC Registry
    walletApiService.fetchWalletDescription();

    try {
      VerifiableCredentialDto vc = verifiableCredentialRegistry.getVerifiableCredential(scope);
      return vc;
    } catch (Exception e) {
      throw new EdcException(e.getMessage());
    }
  }
}
