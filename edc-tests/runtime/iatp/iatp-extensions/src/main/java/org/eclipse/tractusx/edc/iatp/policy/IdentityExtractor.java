/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.iatp.policy;

import org.eclipse.edc.spi.agent.ParticipantAgentServiceExtension;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static org.eclipse.edc.spi.agent.ParticipantAgent.PARTICIPANT_IDENTITY;

public class IdentityExtractor implements ParticipantAgentServiceExtension {
    @Override
    public @NotNull Map<String, String> attributesFor(ClaimToken claimToken) {
        return Map.of(PARTICIPANT_IDENTITY, getClaim(String.class, "holderIdentifier", claimToken.getClaims()));
    }

    protected <T> T getClaim(Class<T> type, String postfix, Map<String, Object> claims) {
        return claims.entrySet().stream().filter(e -> e.getKey().endsWith(postfix))
                .findFirst()
                .map(Map.Entry::getValue)
                .map(type::cast)
                .orElse(null);
    }
}
