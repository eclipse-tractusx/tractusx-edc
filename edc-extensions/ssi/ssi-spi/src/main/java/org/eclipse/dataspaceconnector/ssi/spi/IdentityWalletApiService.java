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

package org.eclipse.dataspaceconnector.ssi.spi;

/**
 * Standard interface for the Wallet API Endpoint, with the needed services
 * from the used Wallet
 * - Verifiable Presentations
 * - Resolve DIDs
 * - Fetches Wallet descriptions into a registry
 * - Finding the EDCs Owner BPNL through the config
 */
public interface IdentityWalletApiService {

  String issueVerifiablePresentation(String verifiableCredentialJson);

  String resolveDid(String did);

  void fetchWalletDescription();

  String validateVerifablePresentation(String verifiablePresentationJson);

  String getOwnerBPN();
}
