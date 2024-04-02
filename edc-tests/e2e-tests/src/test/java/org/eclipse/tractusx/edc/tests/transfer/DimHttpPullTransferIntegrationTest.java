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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.edc.lifecycle.DimParticipant;
import org.eclipse.tractusx.edc.lifecycle.ParticipantRuntime;
import org.eclipse.tractusx.edc.tag.DimIntegrationTest;
import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.helpers.DimHelper.configureParticipant;
import static org.eclipse.tractusx.edc.lifecycle.Runtimes.dimRuntime;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.mockserver.model.HttpRequest.request;

@DimIntegrationTest
@Disabled
public class DimHttpPullTransferIntegrationTest extends HttpConsumerPullBaseTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Integer BDRS_PORT = getFreePort();
    private static final String BDRS_URL = "http://localhost:%s/api".formatted(BDRS_PORT);
    protected static final DimParticipant SOKRATES = configureParticipant(SOKRATES_NAME, BDRS_URL);
    protected static final DimParticipant PLATO = configureParticipant(PLATO_NAME, BDRS_URL);
    @RegisterExtension
    protected static final ParticipantRuntime PLATO_RUNTIME = dimRuntime(PLATO.getName(), PLATO.iatpConfiguration(SOKRATES));
    @RegisterExtension
    protected static final ParticipantRuntime SOKRATES_RUNTIME = dimRuntime(SOKRATES.getName(), SOKRATES.iatpConfiguration(PLATO));
    private static ClientAndServer bdrsServer;

    @BeforeAll
    static void beforeAll() {
        bdrsServer = ClientAndServer.startClientAndServer(BDRS_PORT);
        bdrsServer.when(request()
                        .withMethod("GET")
                        .withPath("/api/bpn-directory"))
                .respond(HttpResponse.response()
                        .withHeader("Content-Encoding", "gzip")
                        .withBody(createGzipStream())
                        .withStatusCode(200));

    }

    private static byte[] createGzipStream() {
        var data = Map.of(SOKRATES.getBpn(), SOKRATES.getDid(),
                PLATO.getBpn(), PLATO.getDid());

        var bas = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(bas)) {
            gzip.write(mapper.writeValueAsBytes(data));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bas.toByteArray();
    }

    @AfterAll
    static void afterAll() {
        bdrsServer.stop();
    }

    @Override
    public TractusxParticipantBase plato() {
        return PLATO;
    }

    @Override
    public TractusxParticipantBase sokrates() {
        return SOKRATES;
    }
}
