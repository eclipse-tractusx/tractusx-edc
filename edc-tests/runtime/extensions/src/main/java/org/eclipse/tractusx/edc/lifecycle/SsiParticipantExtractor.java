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

package org.eclipse.tractusx.edc.lifecycle;

import org.eclipse.edc.spi.agent.ParticipantAgentServiceExtension;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

import static org.eclipse.edc.spi.agent.ParticipantAgent.PARTICIPANT_IDENTITY;
import static org.eclipse.edc.util.reflection.ReflectionUtil.getFieldValue;

public class SsiParticipantExtractor implements ParticipantAgentServiceExtension {

    private static final String EXTRACTING_KEY = "verifiableCredential[0].credentialSubject.holderIdentifier";

    @Override
    public @NotNull Map<String, String> attributesFor(ClaimToken token) {
        var vp = (Map<String, Object>) token.getClaim("vp");
        return Optional.ofNullable(vp)
                .flatMap(this::extractIdentity)
                .map(this::identityMap)
                .orElse(Map.of());
    }

    private Optional<String> extractIdentity(Map<String, Object> vp) {
        return Optional.ofNullable(getFieldValue(EXTRACTING_KEY, vp));
    }

    private Map<String, String> identityMap(String identity) {
        return Map.of(PARTICIPANT_IDENTITY, identity);
    }
    
}
