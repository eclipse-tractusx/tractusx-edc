/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2026 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.discovery.v4alpha;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Violation;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import org.eclipse.tractusx.edc.discovery.v4alpha.api.ConnectorDiscoveryV4AlphaController;
import org.eclipse.tractusx.edc.discovery.v4alpha.exceptions.UnexpectedResultApiException;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryRequest;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryService;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConnectorDiscoveryV4AlphaControllerTest extends RestControllerTestBase {

    private final ConnectorDiscoveryService connectorService = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();
    private final JsonObjectValidatorRegistry validator = mock();
    private final Monitor monitor = mock();

    @Override
    protected Object controller() {
        return new ConnectorDiscoveryV4AlphaController(connectorService, transformerRegistry, validator, monitor);
    }

    @Test
    void connectorParamsDiscovery_shouldReturnSuccess() {
        var input = Json.createObjectBuilder().build();
        var expectedJson = Json.createObjectBuilder()
                .add("counterPartyId", "did:web:provider")
                .add("protocol", "dataspace-protocol-http:2025-1")
                .build();

        var discoveryRequest = new ConnectorParamsDiscoveryRequest("test", "test");

        when(validator.validate(ConnectorParamsDiscoveryRequest.TYPE, input))
                .thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(input, ConnectorParamsDiscoveryRequest.class))
                .thenReturn(Result.success(discoveryRequest));
        when(connectorService.discoverVersionParams(discoveryRequest))
                .thenReturn(CompletableFuture.completedFuture(expectedJson));

        var resultString = baseRequest("/dspversionparams")
                .contentType(ContentType.JSON)
                .body(input)
                .post()
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().asString();

        var resultJson = Json.createReader(new StringReader(resultString)).readObject();

        assertThat(resultJson).isEqualTo(expectedJson);
    }

    @Test
    void connectorParamsDiscovery_shouldReturnFailure_whenServiceFails() {

        var input = Json.createObjectBuilder().build();
        var discoveryRequest = new ConnectorParamsDiscoveryRequest("test", "test");

        when(validator.validate(ConnectorParamsDiscoveryRequest.TYPE, input))
                .thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(input, ConnectorParamsDiscoveryRequest.class))
                .thenReturn(Result.success(discoveryRequest));
        when(connectorService.discoverVersionParams(discoveryRequest))
                .thenReturn(CompletableFuture.failedFuture(new UnexpectedResultApiException("test error")));

        baseRequest("/dspversionparams")
                .contentType(ContentType.JSON)
                .body(input)
                .post()
                .then()
                .log().ifError()
                .statusCode(500);
    }

    @Test
    void connectorParamsDiscovery_shouldReturnFailureBadRequest_whenServiceFails() {

        var input = Json.createObjectBuilder().build();
        var discoveryRequest = new ConnectorParamsDiscoveryRequest("test", "test");

        when(validator.validate(ConnectorParamsDiscoveryRequest.TYPE, input))
                .thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(input, ConnectorParamsDiscoveryRequest.class))
                .thenReturn(Result.success(discoveryRequest));
        when(connectorService.discoverVersionParams(discoveryRequest))
                .thenReturn(CompletableFuture.failedFuture(new InvalidRequestException("test error")));

        baseRequest("/dspversionparams")
                .contentType(ContentType.JSON)
                .body(input)
                .post()
                .then()
                .log().ifError()
                .statusCode(400);
    }

    @Test
    void connectorParamsDiscovery_shouldReturnValidationFailure_whenValidationFails() {

        when(validator.validate(eq(ConnectorParamsDiscoveryRequest.TYPE), any()))
                .thenThrow(new ValidationFailureException(List.of(new Violation("invalidField", "invalidField", "Invalid field"))));

        baseRequest("/dspversionparams")
                .contentType(ContentType.JSON)
                .body("")
                .post()
                .then()
                .log().ifError()
                .statusCode(400);
    }

    @Test
    void connectorServiceDiscovery_shouldReturnSuccess() {
        var input = Json.createObjectBuilder().build();
        var expectedJson = Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add("counterPartyAddress", "https://example.com/api/v1/dsp/2025-1")
                        .add("counterPartyId", "did:web:provider")
                        .add("protocol", "dataspace-protocol-http:2025-1"))
                .build();

        var discoveryRequest = new ConnectorDiscoveryRequest("test", List.of("https://example.com/api/v1/dsp"));

        when(validator.validate(ConnectorDiscoveryRequest.TYPE, input))
                .thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(input, ConnectorDiscoveryRequest.class))
                .thenReturn(Result.success(discoveryRequest));
        when(connectorService.discoverConnectors(discoveryRequest))
                .thenReturn(CompletableFuture.completedFuture(expectedJson));

        var resultString = baseRequest("/connectors")
                .contentType(ContentType.JSON)
                .body(input)
                .post()
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().asString();

        var resultJson = Json.createReader(new StringReader(resultString)).readArray();

        assertThat(resultJson).isEqualTo(expectedJson);
    }

    @Test
    void connectorServiceDiscovery_shouldReturnFailure_whenServiceFails() {

        var input = Json.createObjectBuilder().build();
        var discoveryRequest = new ConnectorDiscoveryRequest("test", List.of("https://example.com/api/v1/dsp"));

        when(validator.validate(ConnectorDiscoveryRequest.TYPE, input))
                .thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(input, ConnectorDiscoveryRequest.class))
                .thenReturn(Result.success(discoveryRequest));
        when(connectorService.discoverConnectors(discoveryRequest))
                .thenReturn(CompletableFuture.failedFuture(new UnexpectedResultApiException("test error")));

        baseRequest("/connectors")
                .contentType(ContentType.JSON)
                .body(input)
                .post()
                .then()
                .log().ifError()
                .statusCode(500);
    }

    @Test
    void connectorServiceDiscovery_shouldReturnFailureBadRequest_whenServiceFails() {

        var input = Json.createObjectBuilder().build();
        var discoveryRequest = new ConnectorDiscoveryRequest("test", List.of("https://example.com/api/v1/dsp"));

        when(validator.validate(ConnectorDiscoveryRequest.TYPE, input))
                .thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(input, ConnectorDiscoveryRequest.class))
                .thenReturn(Result.success(discoveryRequest));
        when(connectorService.discoverConnectors(discoveryRequest))
                .thenReturn(CompletableFuture.failedFuture(new InvalidRequestException("test error")));

        baseRequest("/connectors")
                .contentType(ContentType.JSON)
                .body(input)
                .post()
                .then()
                .log().ifError()
                .statusCode(400);
    }

    @Test
    void connectorServiceDiscovery_shouldReturnValidationFailure_whenValidationFails() {

        when(validator.validate(eq(ConnectorDiscoveryRequest.TYPE), any()))
                .thenThrow(new ValidationFailureException(List.of(new Violation("invalidField", "invalidField", "Invalid field"))));

        baseRequest("/connectors")
                .contentType(ContentType.JSON)
                .body("")
                .post()
                .then()
                .log().ifError()
                .statusCode(400);
    }

    private RequestSpecification baseRequest(String path) {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/v4alpha/connectordiscovery" + path)
                .when();
    }
}
