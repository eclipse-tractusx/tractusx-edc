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
package org.eclipse.tractusx.edc.oauth2.jwt.decorator;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class JWTClaimNames {
  public static final String ISSUER = "iss";
  public static final String SUBJECT = "sub";
  public static final String AUDIENCE = "aud";
  public static final String EXPIRATION_TIME = "exp";
  public static final String NOT_BEFORE = "nbf";
  public static final String ISSUED_AT = "iat";
  public static final String JWT_ID = "jti";
}
