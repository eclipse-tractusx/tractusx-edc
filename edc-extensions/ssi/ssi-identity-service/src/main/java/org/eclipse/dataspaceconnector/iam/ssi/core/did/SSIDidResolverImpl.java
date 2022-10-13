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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.iam.ssi.core.SSIIdentityServiceExtension;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.ssi.spi.IdentityWalletApiService;

/**
 * Resolves the DID with the Wallet Interface
 */
public class SSIDidResolverImpl implements SSIDidResolver{

  private final IdentityWalletApiService walletController;


  public SSIDidResolverImpl(IdentityWalletApiService walletApiService) {
    this.walletController = walletApiService;
  }

  /**
   * Requests the DIDDocoument as Json string and converts it if present
   * otherwise throws an EDC exception
   * @param did as String or BPN
   * @return DIDDocument of a given DID as a String
   * @throws EdcException
   */
  @Override
  public DidDocumentDto resolveDid(String did) throws EdcException {
    String jsonDid = walletController.resolveDid(did);

    if (jsonDid != null){
      try{
        return new ObjectMapper().readValue(jsonDid, DidDocumentDto.class);
      } catch (JsonProcessingException e){
        throw new EdcException("Invalid DidFormat");
      }
    } else {
      throw new EdcException("Did not found");
    }
  }

}
