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

package org.eclipse.tractusx.edc.oauth2;

import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.agent.ParticipantAgentServiceExtension;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.regex.Pattern;

import static org.eclipse.edc.spi.agent.ParticipantAgent.PARTICIPANT_IDENTITY;

public class CxParticipantExtension implements ServiceExtension, ParticipantAgentServiceExtension {

    public static final String REFERRING_CONNECTOR_CLAIM = "referringConnector";

    private static final String DEFAULT_PARTICIPANT_ID_REGEX = "[^/]+(?=/$|$)";
    private static final int DEFAULT_PARTICIPANT_ID_REGEX_GROUP = 0;

    @Setting(value = "Participant Extractor from referringConnector regex", defaultValue = CxParticipantExtension.DEFAULT_PARTICIPANT_ID_REGEX)
    private static final String PARTICIPANT_ID_REGEX = "tx.participant.id.regex";

    @Setting(value = "Participant Extractor from referringConnector regex group", defaultValue = "0")
    private static final String PARTICIPANT_ID_REGEX_GROUP = "tx.participant.id.regexGroup";
    @Inject
    private ParticipantAgentService agentService;
    
    private Pattern participantRegex;

    private int participantRegexGroup;

    @Inject
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        this.participantRegex = Pattern.compile(context.getConfig().getString(PARTICIPANT_ID_REGEX, DEFAULT_PARTICIPANT_ID_REGEX));
        this.participantRegexGroup = context.getConfig().getInteger(PARTICIPANT_ID_REGEX_GROUP, DEFAULT_PARTICIPANT_ID_REGEX_GROUP);

        agentService.register(this);
    }

    @Override
    public @NotNull Map<String, String> attributesFor(ClaimToken token) {
        var referringConnector = token.getClaim(REFERRING_CONNECTOR_CLAIM);
        if (referringConnector instanceof String referringConnectorUrl) {
            var matcher = participantRegex.matcher(referringConnectorUrl);
            if (matcher.find()) {
                var id = matcher.group(participantRegexGroup);
                return Map.of(PARTICIPANT_IDENTITY, id);
            }
            monitor.warning("Unable to extract the participant id from the referring connector claim");
        }
        return Map.of();
    }
}
