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

package org.eclipse.tractusx.edc.lifecycle.tx.iatp;

import org.eclipse.tractusx.edc.lifecycle.tx.TxParticipant;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;

/**
 * STS configurations
 */
public class SecureTokenService {

    protected final URI stsUri = URI.create("http://localhost:" + getFreePort() + "/api/v1/sts");
    protected final TxParticipant stsParticipant = TxParticipant.Builder.newInstance()
            .name("STS")
            .id("STS")
            .build();

    public Map<String, String> stsConfiguration(IatpParticipant... participants) {
        var stsConfiguration = new HashMap<String, String>() {
            {

                put("web.http.sts.port", String.valueOf(stsUri.getPort()));
                put("web.http.sts.path", stsUri.getPath());
                put("edc.dataplane.token.validation.endpoint", "");
                put("tx.vault.seed.secrets", "client_secret_alias:client_secret");
            }
        };

        Arrays.stream(participants).forEach(participant -> {
            var prefix = format("edc.iam.sts.clients.%s", participant.getName().toLowerCase());
            stsConfiguration.put(prefix + ".name", participant.getName());
            stsConfiguration.put(prefix + ".id", participant.didUrl());
            stsConfiguration.put(prefix + ".client_id", participant.getBpn());
            stsConfiguration.put(prefix + ".secret.alias", "client_secret_alias");
            stsConfiguration.put(prefix + ".private-key.alias", participant.verificationId());
        });

        var baseConfiguration = stsParticipant.getConfiguration();
        stsConfiguration.putAll(baseConfiguration);
        return stsConfiguration;
    }

    public String getBpn() {
        return stsParticipant.getBpn();
    }

    public String getName() {
        return stsParticipant.getName();
    }

    public URI stsUri() {
        return stsUri;
    }
}
