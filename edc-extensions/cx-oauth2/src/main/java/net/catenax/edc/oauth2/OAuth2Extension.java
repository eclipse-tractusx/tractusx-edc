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
package net.catenax.edc.oauth2;

import java.net.URI;
import lombok.NonNull;
import lombok.Setter;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.Oauth2JwtDecoratorRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.jwt.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.jwt.TokenValidationService;
import org.eclipse.dataspaceconnector.spi.system.*;

@Provides(IdentityService.class)
@Requires({
  OkHttpClient.class,
  Oauth2JwtDecoratorRegistry.class,
  TokenGenerationService.class,
  TokenValidationService.class
})
public class OAuth2Extension implements ServiceExtension {

  @EdcSetting private static final String TOKEN_URL = "edc.oauth.token.url";

  @EdcSetting private static final String PROVIDER_AUDIENCE = "edc.oauth.provider.audience";

  @Inject @Setter private OkHttpClient okHttpClient;

  @Inject @Setter private Oauth2JwtDecoratorRegistry jwtDecoratorRegistry;

  @Inject @Setter private TokenGenerationService tokenGenerationService;

  @Inject @Setter private TokenValidationService tokenValidationService;

  @Override
  public void initialize(@NonNull final ServiceExtensionContext serviceExtensionContext) {
    final String tokenUrl = serviceExtensionContext.getSetting(TOKEN_URL, null);
    if (tokenUrl == null) {
      throw new EdcException("Missing required setting: " + TOKEN_URL);
    }

    final URI tokenUri = URI.create(tokenUrl);

    final OAuth2IdentityService oAuth2IdentityService =
        new OAuth2IdentityService(
            tokenUri,
            okHttpClient,
            serviceExtensionContext.getTypeManager(),
            jwtDecoratorRegistry,
            tokenGenerationService,
            tokenValidationService);

    serviceExtensionContext.registerService(IdentityService.class, oAuth2IdentityService);
  }
}
