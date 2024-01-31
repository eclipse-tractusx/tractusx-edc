/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.iam.ssi.identity;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.tractusx.edc.iam.ssi.identity.rule.SsiAudienceValidationRule;
import org.eclipse.tractusx.edc.iam.ssi.spi.SsiCredentialClient;
import org.eclipse.tractusx.edc.iam.ssi.spi.SsiTokenValidationService;

import static org.eclipse.tractusx.edc.iam.ssi.spi.SsiConstants.SSI_TOKEN_CONTEXT;

@Provides({ IdentityService.class, SsiTokenValidationService.class, AudienceResolver.class })
@Extension(SsiIdentityServiceExtension.EXTENSION_NAME)
public class SsiIdentityServiceExtension implements ServiceExtension {

    public static final String EXTENSION_NAME = "SSI Identity Service";

    @Setting(value = "SSI Endpoint audience of this connector")
    public static final String ENDPOINT_AUDIENCE = "tx.ssi.endpoint.audience";

    @Inject
    private SsiCredentialClient credentialClient;

    @Inject
    private TokenValidationRulesRegistry rulesRegistry;

    @Override
    public String name() {
        return EXTENSION_NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        var tokenValidationService = new SsiTokenValidationServiceImpl(credentialClient);
        configureRules(context, rulesRegistry);

        var identityService = new SsiIdentityService(tokenValidationService, rulesRegistry, credentialClient);

        context.registerService(IdentityService.class, identityService);
        context.registerService(SsiTokenValidationService.class, tokenValidationService);
        context.registerService(AudienceResolver.class, RemoteMessage::getCounterPartyAddress);
    }

    private void configureRules(ServiceExtensionContext context, TokenValidationRulesRegistry registry) {
        var endpointAudience = context.getConfig().getString(ENDPOINT_AUDIENCE);
        registry.addRule(SSI_TOKEN_CONTEXT, new SsiAudienceValidationRule(endpointAudience));
    }
}
