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

package org.eclipse.tractusx.edc.tests.transfer;

import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.tractusx.edc.tests.extension.VaultSeedExtension;
import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;
import org.eclipse.tractusx.edc.tests.transfer.extension.BdrsServerExtension;
import org.eclipse.tractusx.edc.tests.transfer.extension.DidServerExtension;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.DataspaceIssuer;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpParticipant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpHelperFunctions.configureParticipant;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.runtime.Runtimes.iatpRuntime;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.runtime.Runtimes.stsRuntime;

@EndToEndTest
public class StsConsumerPullTest extends AbstractIatpConsumerPullTest {

    @RegisterExtension
    private static final DidServerExtension DID_SERVER = new DidServerExtension();

    private static final DataspaceIssuer DATASPACE_ISSUER_PARTICIPANT = new DataspaceIssuer(DID_SERVER.didFor("issuer"));

    private static final IatpParticipant CONSUMER = IatpParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_BPN)
            .stsUri(STS.stsUri())
            .stsClientId(CONSUMER_BPN)
            .trustedIssuer(DATASPACE_ISSUER_PARTICIPANT.didUrl())
            .did(DID_SERVER.didFor(CONSUMER_NAME))
            .build();
    private static final IatpParticipant PROVIDER = IatpParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_BPN)
            .stsUri(STS.stsUri())
            .stsClientId(PROVIDER_BPN)
            .trustedIssuer(DATASPACE_ISSUER_PARTICIPANT.didUrl())
            .did(DID_SERVER.didFor(PROVIDER_NAME))
            .build();

    @RegisterExtension
    private static final BdrsServerExtension BDRS_SERVER_EXTENSION = new BdrsServerExtension(DATASPACE_ISSUER_PARTICIPANT.didUrl());

    @RegisterExtension
    private static final RuntimeExtension CONSUMER_RUNTIME = iatpRuntime(CONSUMER.getName(), CONSUMER.getKeyPair(),
            () -> CONSUMER.iatpConfig().merge(BDRS_SERVER_EXTENSION.getConfig()));

    @RegisterExtension
    private static final RuntimeExtension PROVIDER_RUNTIME = iatpRuntime(PROVIDER.getName(), PROVIDER.getKeyPair(),
            () -> PROVIDER.iatpConfig().merge(BDRS_SERVER_EXTENSION.getConfig()))
            .registerSystemExtension(ServiceExtension.class, new VaultSeedExtension(Map.of("client_secret_alias", "client_secret")));

    @RegisterExtension
    private static final RuntimeExtension STS_RUNTIME = stsRuntime(STS.getName(), STS.getKeyPair(),
            () -> STS.stsConfig(CONSUMER, PROVIDER).merge(BDRS_SERVER_EXTENSION.getConfig()))
            .registerSystemExtension(ServiceExtension.class, new VaultSeedExtension(Map.of("client_secret_alias", "client_secret")));

    @BeforeAll
    static void beforeAll() {
        DID_SERVER.register(CONSUMER_NAME, CONSUMER.getDidDocument());
        DID_SERVER.register(PROVIDER_NAME, PROVIDER.getDidDocument());
        DID_SERVER.register("issuer", DATASPACE_ISSUER_PARTICIPANT.didDocument());

        BDRS_SERVER_EXTENSION.addMapping(CONSUMER.getBpn(), CONSUMER.getDid());
        BDRS_SERVER_EXTENSION.addMapping(PROVIDER.getBpn(), PROVIDER.getDid());
    }

    // credentials etc get wiped after every, so the need to be created before every test
    @BeforeEach
    void setUp() {
        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, CONSUMER, CONSUMER_RUNTIME, STS_RUNTIME);
        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, PROVIDER, PROVIDER_RUNTIME, STS_RUNTIME);
    }

    @Override
    protected RuntimeExtension consumerRuntime() {
        return CONSUMER_RUNTIME;
    }

    @Override
    protected RuntimeExtension providerRuntime() {
        return PROVIDER_RUNTIME;
    }

    @Override
    protected DataspaceIssuer dataspaceIssuer() {
        return DATASPACE_ISSUER_PARTICIPANT;
    }

    @Override
    public TractusxParticipantBase provider() {
        return PROVIDER;
    }

    @Override
    public TractusxParticipantBase consumer() {
        return CONSUMER;
    }
}
