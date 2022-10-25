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
package org.eclipse.tractusx.edc.oauth2;

import java.util.Map;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.CredentialsRequestAdditionalParametersProvider;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;

public class CXOAuth2Extension implements ServiceExtension {

  @Override
  public String name() {
    return "CX OAuth2";
  }

  @Provider
  public CredentialsRequestAdditionalParametersProvider
      credentialsRequestAdditionalParametersProvider() {
    return tokenParameters -> Map.of("resource", tokenParameters.getAudience());
  }
}
