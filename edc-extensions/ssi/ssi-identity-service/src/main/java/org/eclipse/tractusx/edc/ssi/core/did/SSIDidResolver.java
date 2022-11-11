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
import org.eclipse.dataspaceconnector.spi.EdcException;

public interface SSIDidResolver {

  /**
   * Requests the DIDDocument as Json string and converts it if present otherwise throws an EDC
   * exception
   *
   * @param did as String or BPN
   * @return DIDDocument of a given DID as a String
   * @throws EdcException
   */
  DidDocumentDto resolveDid(String did) throws JsonProcessingException;
}
