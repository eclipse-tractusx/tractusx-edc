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

package org.eclipse.tractusx.edc.tests.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.edc.lifecycle.DimParticipant;
import org.eclipse.tractusx.edc.lifecycle.ParticipantRuntime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;
import static org.eclipse.tractusx.edc.helpers.DimHelper.configureParticipant;
import static org.eclipse.tractusx.edc.lifecycle.Runtimes.dimRuntime;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.CatalogHelperFunctions.getDatasetAssetId;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.frameworkPolicy;
import static org.mockserver.model.HttpRequest.request;

//@DimIntegrationTest
//@Disabled
public class DimCatalogIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Integer BDRS_PORT = getFreePort();
    private static final String BDRS_URL = "http://localhost:%s/api".formatted(BDRS_PORT);

    protected static final DimParticipant CONSUMER = configureParticipant(CONSUMER_NAME, BDRS_URL);
    protected static final DimParticipant PROVIDER = configureParticipant(PROVIDER_NAME, BDRS_URL);

    @RegisterExtension
    protected static final ParticipantRuntime PROVIDER_RUNTIME = dimRuntime(PROVIDER.getName(), PROVIDER.iatpConfiguration(CONSUMER));

    @RegisterExtension
    protected static final ParticipantRuntime CONSUMER_RUNTIME = dimRuntime(CONSUMER.getName(), CONSUMER.iatpConfiguration(PROVIDER));
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
        var data = Map.of(CONSUMER.getBpn(), CONSUMER.getDid(),
                PROVIDER.getBpn(), PROVIDER.getDid());

        var bas = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(bas)) {
            gzip.write(MAPPER.writeValueAsBytes(data));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bas.toByteArray();
    }

    @AfterAll
    static void afterAll() {
        bdrsServer.stop();
    }

    //@Test
    @DisplayName("Verify that the consumer receives only the offers he is permitted to")
    void requestCatalog_filteredByDismantler_shouldReturnOffer() {
        // arrange
        PROVIDER.createAsset("test-asset");
        PROVIDER.createAsset("test-asset-1");

        var bpnAccessPolicy = frameworkPolicy(Map.of(CX_POLICY_NS + "Membership", "active"));
        var dismantlerAccessPolicy = frameworkPolicy(Map.of(CX_POLICY_NS + "Dismantler", "active"));

        var bpnAccessId = PROVIDER.createPolicyDefinition(bpnAccessPolicy);
        var contractPolicyId = PROVIDER.createPolicyDefinition(noConstraintPolicy());
        var dismantlerAccessPolicyId = PROVIDER.createPolicyDefinition(dismantlerAccessPolicy);

        PROVIDER.createContractDefinition("test-asset", "test-def", bpnAccessId, contractPolicyId);
        PROVIDER.createContractDefinition("test-asset-1", "test-def-2", dismantlerAccessPolicyId, contractPolicyId);

        // act
        var catalog = CONSUMER.getCatalogDatasets(PROVIDER);

        // assert
        assertThat(catalog).isNotEmpty()
                .hasSize(1)
                .allSatisfy(co -> {
                    assertThat(getDatasetAssetId(co.asJsonObject())).isEqualTo("test-asset");
                });

    }

}
