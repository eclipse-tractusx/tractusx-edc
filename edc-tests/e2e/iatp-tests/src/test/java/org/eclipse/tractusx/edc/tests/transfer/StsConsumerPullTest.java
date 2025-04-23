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

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.tractusx.edc.tests.extension.VaultSeedExtension;
import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpParticipant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
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

    protected static final IatpParticipant CONSUMER = IatpParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_BPN)
            .stsUri(STS.stsUri())
            .stsClientId(CONSUMER_BPN)
            .trustedIssuer(DATASPACE_ISSUER_PARTICIPANT.didUrl())
            .did(did(CONSUMER_NAME))
            .build();
    protected static final IatpParticipant PROVIDER = IatpParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_BPN)
            .stsUri(STS.stsUri())
            .stsClientId(PROVIDER_BPN)
            .trustedIssuer(DATASPACE_ISSUER_PARTICIPANT.didUrl())
            .did(did(PROVIDER_NAME))
            .build();

    @RegisterExtension
    protected static final RuntimeExtension CONSUMER_RUNTIME = iatpRuntime(CONSUMER.getName(), CONSUMER.getKeyPair(), () -> CONSUMER.iatpConfig(PROVIDER));

    @RegisterExtension
    protected static final RuntimeExtension PROVIDER_RUNTIME = iatpRuntime(PROVIDER.getName(), PROVIDER.getKeyPair(), () -> PROVIDER.iatpConfig(CONSUMER))
            .registerSystemExtension(ServiceExtension.class, new VaultSeedExtension(Map.of("client_secret_alias", "client_secret")));

    @RegisterExtension
    protected static final RuntimeExtension STS_RUNTIME = stsRuntime(STS.getName(), STS.getKeyPair(), () -> STS.stsConfig(CONSUMER, PROVIDER))
            .registerSystemExtension(ServiceExtension.class, new VaultSeedExtension(Map.of("client_secret_alias", "client_secret")));

    @BeforeEach
    void prepare() {
        var dids = new HashMap<String, DidDocument>();
        dids.put(DATASPACE_ISSUER_PARTICIPANT.didUrl(), DATASPACE_ISSUER_PARTICIPANT.didDocument());
        dids.put(CONSUMER.getDid(), CONSUMER.getDidDocument());
        dids.put(PROVIDER.getDid(), PROVIDER.getDidDocument());

        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, CONSUMER, CONSUMER_RUNTIME, dids, STS_RUNTIME);
        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, PROVIDER, PROVIDER_RUNTIME, dids, STS_RUNTIME);
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
    public TractusxParticipantBase provider() {
        return PROVIDER;
    }

    @Override
    public TractusxParticipantBase consumer() {
        return CONSUMER;
    }
}
