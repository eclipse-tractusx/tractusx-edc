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
package org.eclipse.tractusx.edc.ssi.spi;

/**
 * Standard interface for the Wallet API Endpoint, with the needed services from the used Wallet -
 * Verifiable Presentations - Resolve DIDs - Fetches Wallet descriptions into a registry - Finding
 * the EDCs Owner BPNL through the config
 */
public interface IdentityWalletApiService {

  /**
   * Issue a Verifiable Presentation for the given Verifiable Credential.
   *
   * @param verifiableCredentialJson the verifiable Credentials in Json format as String
   * @return The Verifiable Presentation in Json format as String
   */
  String issueVerifiablePresentation(String verifiableCredentialJson);

  /**
   * Resolve the DID Document for a given DID
   *
   * @param did the given DID as String
   * @return the DID Document in Json format as String
   */
  String resolveDid(String did);

  /** Fetch the description of owned wallet and store its credentials in the Registry. */
  void fetchWalletDescription();

  /**
   * Validate the Verifiable Presentation and check if its credentials are signed by one of the
   * trusted issuer.
   *
   * @param verifiablePresentationJson the verifiable presentation as String
   * @return True if valid, otherwise false
   */
  boolean validateVerifiablePresentation(String verifiablePresentationJson);

  /**
   * Get the BPN of Owner.
   *
   * @return the BPN of the owner as String
   */
  String getOwnerBPN();
}
