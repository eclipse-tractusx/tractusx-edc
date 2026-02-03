/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
 * Copyright (c) 2026 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.tests.runtimes;

import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.protocol.spi.DefaultParticipantIdExtractionFunction;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.eclipse.tractusx.edc.tests.MockBdrsClient;
import org.eclipse.tractusx.edc.tests.MockIdentityServiceExtension;
import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.BPN_PREFIX;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.BPN_SUFFIX;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DID_PREFIX;

public interface Runtimes {

    static RuntimeExtension pgRuntime(TractusxParticipantBase participant, PostgresExtension postgres) {
        return pgRuntime(participant, postgres, ConfigFactory::empty);
    }

    static RuntimeExtension pgRuntime(TractusxParticipantBase participant, PostgresExtension postgres, Supplier<Config> configurationProvider) {
        Function<String, String> bpnToDid = bpn -> DID_PREFIX + bpn.replace(BPN_SUFFIX, "");
        Function<String, String> didToBpn = did -> BPN_PREFIX + did.replace(DID_PREFIX, "");
        return new ParticipantRuntimeExtension(
                new EmbeddedRuntime(participant.getName(), ":edc-tests:runtime:runtime-postgresql")
                        .configurationProvider(() -> participant.getConfig().merge(postgres.getConfig(participant.getName())))
                        .configurationProvider(configurationProvider)
                        .registerServiceMock(AudienceResolver.class, remoteMessage -> Result.success(remoteMessage.getCounterPartyAddress()))
                        .registerServiceMock(BdrsClient.class, new MockBdrsClient(bpnToDid, didToBpn))
                        .registerSystemExtension(ServiceExtension.class, new MockIdentityServiceExtension(participant.getBpn(), participant.getDid()))
                        .registerServiceMock(DefaultParticipantIdExtractionFunction.class, ct -> "id")
        );
    }

    static RuntimeExtension discoveryRuntimeFullDsp(TractusxParticipantBase participant) {
        return discoveryRuntime(participant, ":edc-tests:runtime:runtime-discovery:runtime-discovery-base");
    }

    static RuntimeExtension discoveryRuntimeDsp08(TractusxParticipantBase participant) {
        return discoveryRuntime(participant, ":edc-tests:runtime:runtime-discovery:runtime-discovery-with-dsp-v08");
    }

    static RuntimeExtension discoveryRuntimeNoProtocols(TractusxParticipantBase participant) {
        return discoveryRuntime(participant, ":edc-tests:runtime:runtime-discovery:runtime-discovery-no-protocols");
    }

    static RuntimeExtension discoveryRuntime(TractusxParticipantBase participant, String module) {
        return new ParticipantRuntimeExtension(new EmbeddedRuntime(participant.getName(), module)
                .registerSystemExtension(ServiceExtension.class, new MockIdentityServiceExtension(participant.getBpn(), participant.getDid()))
                .registerServiceMock(AudienceResolver.class, remoteMessage -> Result.success(remoteMessage.getCounterPartyAddress()))
                .configurationProvider(participant::getConfig));
    }

}
