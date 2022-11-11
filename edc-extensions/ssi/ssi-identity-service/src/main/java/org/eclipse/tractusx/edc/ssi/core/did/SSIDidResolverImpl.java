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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.tractusx.edc.ssi.spi.IdentityWalletApiService;

/** Resolves the DID with the Wallet Interface */
public class SSIDidResolverImpl implements SSIDidResolver {

  private final IdentityWalletApiService walletController;

  public SSIDidResolverImpl(IdentityWalletApiService walletApiService) {
    this.walletController = walletApiService;
  }

  @Override
  public DidDocumentDto resolveDid(String did) throws EdcException {
    String jsonDid = walletController.resolveDid(did);

    if (jsonDid != null) {
      try {
        return new ObjectMapper().readValue(jsonDid, DidDocumentDto.class);
      } catch (JsonProcessingException e) {
        throw new EdcException("Invalid DidFormat");
      }
    } else {
      throw new EdcException("Did not found");
    }
  }
}
