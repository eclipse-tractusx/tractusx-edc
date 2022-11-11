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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.tractusx.edc.ssi.core.did.SSIDidResolver;
import org.eclipse.tractusx.edc.ssi.core.did.SSIDidResolverImpl;
import org.eclipse.tractusx.edc.ssi.miw.model.VerifiablePresentationDto;
import org.eclipse.tractusx.edc.ssi.spi.IdentityWalletApiService;

/** Verification of the signature validation from a given Verifiable Presentation */
public class SSIVerificationImpl implements SSIVerification {

  private final SSIDidResolver didResolver;
  private final IdentityWalletApiService walletApiService;

  public SSIVerificationImpl(IdentityWalletApiService walletApiService) {
    didResolver = new SSIDidResolverImpl(walletApiService);
    this.walletApiService = walletApiService;
  }

  @Override
  public boolean verifyPresentation(VerifiablePresentationDto vp) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      String jsonVP = mapper.writeValueAsString(vp);
      return walletApiService.validateVerifiablePresentation(jsonVP);
    } catch (Exception e) {
      throw new EdcException(e.getMessage());
    }
  }
}
