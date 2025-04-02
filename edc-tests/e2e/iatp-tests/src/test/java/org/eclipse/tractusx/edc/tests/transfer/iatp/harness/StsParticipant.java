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


import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * STS configurations
 */
public class StsParticipant extends TractusxParticipantBase {

    protected final LazySupplier<URI> stsUri = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/api/v1/sts"));

    private StsParticipant() {
    }

    public Config stsConfig(IatpParticipant... participants) {
        var additionalSettings = Map.of(
                "web.http.sts.port", String.valueOf(stsUri.get().getPort()),
                "web.http.sts.path", stsUri.get().getPath()
        );

        var baseConfig = super.getConfig()
                .merge(ConfigFactory.fromMap(additionalSettings));

        return Arrays.stream(participants)
                .map(participant -> {
                    var prefix = format("edc.iam.sts.clients.%s", participant.getName().toLowerCase());
                    return Map.of(
                            prefix + ".name", participant.getName(),
                            prefix + ".id", UUID.randomUUID().toString(),
                            prefix + ".client_id", participant.getBpn(),
                            prefix + ".did", participant.getDid(),
                            prefix + ".secret.alias", "client_secret_alias",
                            prefix + ".private-key.alias", participant.verificationId(),
                            prefix + ".public-key.reference", participant.verificationId()
                    );
                })
                .map(ConfigFactory::fromMap)
                .reduce(baseConfig, Config::merge);
    }

    @Override
    public String getFullKeyId() {
        return "sts-" + getKeyId();
    }

    public LazySupplier<URI> stsUri() {
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
