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

package org.eclipse.tractusx.edc.tests.runtimes;

import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.eclipse.tractusx.edc.tests.MockBpnIdentityService;
import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;

import java.util.function.Supplier;

public interface Runtimes {

    static RuntimeExtension pgRuntime(TractusxParticipantBase participant, PostgresExtension postgres) {
        return pgRuntime(participant, postgres, ConfigFactory::empty);
    }

    static RuntimeExtension pgRuntime(TractusxParticipantBase participant, PostgresExtension postgres, Supplier<Config> configurationProvider) {
        return new ParticipantRuntimeExtension(
                new EmbeddedRuntime(participant.getName(), ":edc-tests:runtime:runtime-postgresql")
                        .configurationProvider(() -> participant.getConfig().merge(postgres.getConfig(participant.getName())))
                        .configurationProvider(configurationProvider)
                        .registerServiceMock(IdentityService.class, new MockBpnIdentityService(participant.getBpn()))
                        .registerServiceMock(AudienceResolver.class, remoteMessage -> Result.success(remoteMessage.getCounterPartyAddress()))
                        .registerServiceMock(BdrsClient.class, (s) -> s)
        );
    }
}
