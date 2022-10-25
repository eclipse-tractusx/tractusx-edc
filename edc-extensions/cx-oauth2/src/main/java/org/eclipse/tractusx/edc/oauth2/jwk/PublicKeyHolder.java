/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */
package org.eclipse.tractusx.edc.oauth2.jwk;

import java.security.PublicKey;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicKeyHolder {
  private final String keyId;
  private final PublicKey publicKey;
}
