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

import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.DataspaceIssuer;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpParticipant;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.StsParticipant;

import java.net.URI;

import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;

public interface IatpParticipants {

    LazySupplier<URI> DIM_URI = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort()));
    DataspaceIssuer DATASPACE_ISSUER_PARTICIPANT = new DataspaceIssuer();
    StsParticipant STS = StsParticipant.Builder.newInstance()
            .id("STS")
            .name("STS")
            .build();
    IatpParticipant CONSUMER = IatpParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_BPN)
            .stsUri(STS.stsUri())
            .stsClientId(CONSUMER_BPN)
            .trustedIssuer(DATASPACE_ISSUER_PARTICIPANT.didUrl())
            .dimUri(DIM_URI)
            .did(did(CONSUMER_NAME))
            .build();

    IatpParticipant PROVIDER = IatpParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_BPN)
            .stsUri(STS.stsUri())
            .stsClientId(PROVIDER_BPN)
            .trustedIssuer(DATASPACE_ISSUER_PARTICIPANT.didUrl())
            .dimUri(DIM_URI)
            .did(did(PROVIDER_NAME))
            .build();

    static String did(String name) {
        return "did:example:" + name.toLowerCase();
    }

}
