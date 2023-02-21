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
package net.catenax.edc.oauth2.jwt.generator;

import java.security.PrivateKey;
import lombok.NonNull;
import lombok.Setter;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.jwt.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.*;

@Provides(TokenGenerationService.class)
@Requires(PrivateKeyResolver.class)
public class JwtTokenGenerationServiceExtension implements ServiceExtension {

  @EdcSetting private static final String PRIVATE_KEY_ALIAS = "edc.oauth.private.key.alias";

  @Inject @Setter private PrivateKeyResolver privateKeyResolver;

  @Override
  public void initialize(@NonNull final ServiceExtensionContext serviceExtensionContext) {
    final PrivateKey privateKey = privateKey(serviceExtensionContext);
    final TokenGenerationService tokenGenerationService = new JwtTokenGenerationService(privateKey);

    serviceExtensionContext.registerService(TokenGenerationService.class, tokenGenerationService);
  }

  private PrivateKey privateKey(final ServiceExtensionContext serviceExtensionContext) {
    final String privateKeyAlias = serviceExtensionContext.getConfig().getString(PRIVATE_KEY_ALIAS);
    return privateKeyResolver.resolvePrivateKey(privateKeyAlias, PrivateKey.class);
  }
}
