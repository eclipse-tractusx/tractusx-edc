/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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
 */

package org.eclipse.tractusx.edc.samples.mockedc;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * This example demonstrates how to use the Mock-Connector as a drop-in replacement runtime for testing client code that uses EDC's
 * Management API. While this is written in Java, the concepts are easily translatable into any language where test containers are
 * supported.
 */
@Testcontainers
@ComponentTest
public class UseMockConnectorSampleTest {

    private static final int DEFAULT_PORT = 8080;
    private static final int MANAGEMENT_PORT = 8081;

    @Container
    private final GenericContainer<?> edcContainer = new GenericContainer<>("mock-connector")
            .withEnv("WEB_HTTP_PORT", String.valueOf(DEFAULT_PORT))
            .withEnv("WEB_HTTP_PATH", "/api")
            .withEnv("WEB_HTTP_MANAGEMENT_PORT", String.valueOf(MANAGEMENT_PORT))
            .withEnv("WEB_HTTP_MANAGEMENT_PATH", "/api/management")
            .withExposedPorts(DEFAULT_PORT, MANAGEMENT_PORT)
            .waitingFor(Wait.forLogMessage(".* ready.*", 1));

    @Test
    void test_getAsset() {
        //prime the mock - post a RecordedRequest
        setupNextResponse("asset.request.json");

        // perform the actual Asset API request. In a real test scenario, this would be the client code we're testing, i.e. the
        // System-under-Test (SuT).
        var assetArray = mgmtRequest()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "@context": {
                            "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
                          },
                        "@type": "QuerySpec"
                        }
                        """)
                .post("/v3/assets/request")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(JsonArray.class);

        // assert the response
        assertThat(assetArray).hasSize(1);
        assertThat(assetArray.get(0).asJsonObject().get("properties"))
                .hasFieldOrProperty("prop1")
                .hasFieldOrProperty("id")
                .hasFieldOrProperty("contenttype");
    }

    @Test
    void test_apiNotAuthenticated_expect400() {
        //prime the mock - post a RecordedRequest
        setupNextResponse("asset.creation.failure.json");

        // perform the actual Asset API request. In a real test scenario, this would be the client code we're testing, i.e. the
        // System-under-Test (SuT).
        var assetArray = mgmtRequest()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "@context": {
                            "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
                          },
                        "@type": "QuerySpec"
                        }
                        """)
                .post("/v3/assets/request")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .extract().body().as(JsonArray.class);

        // assert the response contains error information
        assertThat(assetArray).hasSize(1);
        var errorObject = assetArray.get(0).asJsonObject();
        assertThat(errorObject.get("message").toString()).contains("This user is not authorized, This is just a second error message");
    }

    @Test
    void test_getProtocolVersions() {
        setupNextResponse("versions.request.json");
        var response = mgmtRequest()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "@context": {
                            "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
                          },
                        "@type": "QuerySpec",
                        "https://w3id.org/edc/v0.0.1/ns/counterPartyAddress": "http://provider-control-plane:8282/api/v1/dsp",
                        "https://w3id.org/edc/v0.0.1/ns/counterPartyId": "providerId",
                        "https://w3id.org/edc/v0.0.1/ns/protocol": "dataspace-protocol-http"
                        }
                        """)
                .post("/v4alpha/protocol-versions/request")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract()
                .body()
                .as(JsonObject.class);

        var protocolVersions = response.get("protocolVersions").asJsonArray();

        assertThat(protocolVersions).hasSize(2);
        assertThat(protocolVersions.getJsonObject(0).getJsonString("version").getString()).isEqualTo("2024/1");
        assertThat(protocolVersions.getJsonObject(0).getJsonString("path").getString()).isEqualTo("/2024/1");
        assertThat(protocolVersions.getJsonObject(1).getJsonString("version").getString()).isEqualTo("v0.8");
        assertThat(protocolVersions.getJsonObject(1).getJsonString("path").getString()).isEqualTo("/");
    }

    private void setupNextResponse(String resourceFileName) {
        var json = TestUtils.getResourceFileContentAsString(resourceFileName);

        apiRequest()
                .contentType(ContentType.JSON)
                .body(json)
                .post("/instrumentation")
                .then()
                .log().ifValidationFails()
                .statusCode(204);
    }

    private RequestSpecification apiRequest() {
        return given()
                .baseUri("http://localhost:" + edcContainer.getMappedPort(DEFAULT_PORT) + "/api")
                .when();
    }

    private RequestSpecification mgmtRequest() {
        return given()
                .baseUri("http://localhost:" + edcContainer.getMappedPort(MANAGEMENT_PORT) + "/api/management")
                .when();
    }
}
