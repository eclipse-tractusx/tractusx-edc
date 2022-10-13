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

public interface SSIDidResolver {

  /**
   * Resolve a DID or BPN via the MIW API endpoint
   *
   * @param did as String or BPN
   * @return DidDocumentDto
   */
  DidDocumentDto resolveDid(String did) throws JsonProcessingException;
}
