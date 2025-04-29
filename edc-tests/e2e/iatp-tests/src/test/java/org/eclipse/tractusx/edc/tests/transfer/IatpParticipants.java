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

public interface IatpParticipants {

    LazySupplier<URI> DIM_URI = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort()));
    DataspaceIssuer DATASPACE_ISSUER_PARTICIPANT = new DataspaceIssuer();
    StsParticipant STS = StsParticipant.Builder.newInstance()
            .id("STS")
            .name("STS")
            .build();

    static IatpParticipant participant(String name, String bpn) {
        return IatpParticipant.Builder.newInstance()
                .name(name)
                .id(bpn)
                .stsUri(STS.stsUri())
                .stsClientId(bpn)
                .trustedIssuer(DATASPACE_ISSUER_PARTICIPANT.didUrl())
                .dimUri(DIM_URI)
                .did(did(name))
                .build();
    }

    static String did(String name) {
        return "did:example:" + name.toLowerCase();
    }

}
