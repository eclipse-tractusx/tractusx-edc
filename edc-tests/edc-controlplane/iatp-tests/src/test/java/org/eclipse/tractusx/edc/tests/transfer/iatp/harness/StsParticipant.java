/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.edc.tests.transfer.iatp.harness;


import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * STS configurations
 */
public class StsParticipant extends TractusxParticipantBase {

    protected final URI stsUri = URI.create("http://localhost:" + getFreePort() + "/api/v1/sts");

    private StsParticipant() {
    }

    public Map<String, String> stsConfiguration(IatpParticipant... participants) {
        var stsConfiguration = new HashMap<>(super.getConfiguration());

        stsConfiguration.put("web.http.sts.port", String.valueOf(stsUri.getPort()));
        stsConfiguration.put("web.http.sts.path", stsUri.getPath());
        stsConfiguration.put("tx.vault.seed.secrets", "client_secret_alias:client_secret");

        Arrays.stream(participants).forEach(participant -> {
            var prefix = format("edc.iam.sts.clients.%s", participant.getName().toLowerCase());
            stsConfiguration.put(prefix + ".name", participant.getName());
            stsConfiguration.put(prefix + ".id", UUID.randomUUID().toString());
            stsConfiguration.put(prefix + ".client_id", participant.getBpn());
            stsConfiguration.put(prefix + ".did", participant.getDid());
            stsConfiguration.put(prefix + ".secret.alias", "client_secret_alias");
            stsConfiguration.put(prefix + ".private-key.alias", participant.verificationId());
            stsConfiguration.put(prefix + ".public-key.reference", participant.verificationId());
        });

        return stsConfiguration;
    }


    @Override
    public String getFullKeyId() {
        return "sts-" + getKeyId();
    }


    public URI stsUri() {
        return stsUri;
    }

    public static class Builder extends TractusxParticipantBase.Builder<StsParticipant, Builder> {

        protected Builder() {
            super(new StsParticipant());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public StsParticipant build() {
            super.build();
            return participant;
        }
    }
}
