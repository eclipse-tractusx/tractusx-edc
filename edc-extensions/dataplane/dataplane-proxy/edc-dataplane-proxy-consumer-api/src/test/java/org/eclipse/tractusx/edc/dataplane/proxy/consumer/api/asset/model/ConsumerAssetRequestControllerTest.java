/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.specification.RequestSpecification;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset.ClientErrorExceptionMapper;
import org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset.ConsumerAssetRequestController;
import org.eclipse.tractusx.edc.edr.spi.service.EdrService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.spi.schema.DataFlowRequestSchema.PATH;
import static org.eclipse.edc.connector.dataplane.spi.schema.DataFlowRequestSchema.QUERY_PARAMS;
import static org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset.ConsumerAssetRequestController.BASE_URL;
import static org.eclipse.tractusx.edc.edr.spi.types.RefreshMode.AUTO_REFRESH;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiTest
public class ConsumerAssetRequestControllerTest extends RestControllerTestBase {

    public static final String ASSET_REQUEST_PATH = "/aas/request";
    private final EdrService edrService = mock(EdrService.class);
    private final PipelineService pipelineService = mock();
    private final ObjectMapper mapper = new ObjectMapper();

    private static Stream<Arguments> provideServiceResultForProxyCall() {
        return Stream.of(
                Arguments.of(StreamResult.notFound(), NOT_FOUND.getStatusCode()),
                Arguments.of(StreamResult.notAuthorized(), UNAUTHORIZED.getStatusCode()),
                Arguments.of(StreamResult.error("error"), INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    void requestAsset_shouldReturnData_withAssetId() throws IOException {

        var assetId = "assetId";
        var transferProcessId = "tp";
        var request = Map.of("assetId", assetId);

        var response = Map.of("response", "ok");
        var responseBytes = mapper.writeValueAsBytes(response);

        var datasource = mock(DataSource.class);
        var partStream = mock(DataSource.Part.class);

        when(datasource.openPartStream()).thenReturn(StreamResult.success(Stream.of(partStream)));
        when(partStream.openStream()).thenReturn(new ByteArrayInputStream(responseBytes));

        when(edrService.query(argThat(queryContainsFilter("assetId")))).thenReturn(ServiceResult.success(List.of(edrEntry(assetId, transferProcessId))));
        when(edrService.resolveByTransferProcess(transferProcessId, AUTO_REFRESH)).thenReturn(ServiceResult.success(edr()));
        when(pipelineService.transfer(any(), any()))
                .thenAnswer(a -> CompletableFuture.completedFuture(StreamResult.success(response)));

        var proxyResponseBytes = baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(ASSET_REQUEST_PATH)
                .then()
                .statusCode(200)
                .extract().body().asByteArray();

        var proxyResponse = mapper.readValue(proxyResponseBytes, new TypeReference<Map<String, String>>() {
        });

        assertThat(proxyResponse).containsAllEntriesOf(response);
    }

    @Test
    void requestAsset_shouldReturnData_withAssetIdAndProviderId() throws IOException {

        var assetId = "assetId";
        var transferProcessId = "tp";
        var providerId = "providerId";
        var request = Map.of("assetId", assetId, "providerId", providerId);

        var response = Map.of("response", "ok");
        var responseBytes = mapper.writeValueAsBytes(response);

        var datasource = mock(DataSource.class);
        var partStream = mock(DataSource.Part.class);

        when(datasource.openPartStream()).thenReturn(StreamResult.success(Stream.of(partStream)));
        when(partStream.openStream()).thenReturn(new ByteArrayInputStream(responseBytes));

        when(edrService.query(argThat(queryContainsFilter("assetId", "providerId")))).thenReturn(ServiceResult.success(List.of(edrEntry(assetId, transferProcessId, providerId))));
        when(edrService.resolveByTransferProcess(transferProcessId, AUTO_REFRESH)).thenReturn(ServiceResult.success(edr()));
        when(pipelineService.transfer(any(), any()))
                .thenAnswer(a -> CompletableFuture.completedFuture(StreamResult.success(response)));

        var proxyResponseBytes = baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(ASSET_REQUEST_PATH)
                .then()
                .statusCode(200)
                .extract().body().asByteArray();

        var proxyResponse = mapper.readValue(proxyResponseBytes, new TypeReference<Map<String, String>>() {
        });

        assertThat(proxyResponse).containsAllEntriesOf(response);
    }

    @ParameterizedTest
    @MethodSource("provideServiceResultForProxyCall")
    void requestAsset_shouldReturnError_WhenProxyCallFails(StreamResult<Object> result, Integer responseCode) throws IOException {

        var assetId = "assetId";
        var transferProcessId = "tp";
        var request = Map.of("assetId", assetId);

        when(edrService.query(any())).thenReturn(ServiceResult.success(List.of(edrEntry(assetId, transferProcessId))));
        when(edrService.resolveByTransferProcess(transferProcessId, AUTO_REFRESH)).thenReturn(ServiceResult.success(edr()));
        when(pipelineService.transfer(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(result));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(ASSET_REQUEST_PATH)
                .then()
                .statusCode(responseCode);

    }

    @Test
    void requestAsset_shouldReturnError_whenEdrByAssetIdNotFound() {

        var assetId = "assetId";
        var url = "http://localhost:8080/test";
        var request = Map.of("assetId", assetId);

        when(edrService.query(any())).thenReturn(ServiceResult.success(List.of()));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(ASSET_REQUEST_PATH)
                .then()
                .statusCode(400)
                .body("message", notNullValue());

    }

    @Test
    void requestAsset_shouldReturnError_whenMultipleEdrsByAssetIdFound() {

        var assetId = "assetId";
        var request = Map.of("assetId", assetId);

        when(edrService.query(any())).thenReturn(ServiceResult.success(List.of(edrEntry(assetId), edrEntry(assetId))));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(ASSET_REQUEST_PATH)
                .then()
                .statusCode(428)
                .body("message", notNullValue());

    }

    @Test
    void requestAsset_shouldReturnData_withTransferProcessId() throws IOException {

        var transferProcessId = "tp";
        var request = Map.of("transferProcessId", transferProcessId);

        var response = Map.of("response", "ok");
        var responseBytes = mapper.writeValueAsBytes(response);

        var datasource = mock(DataSource.class);
        var partStream = mock(DataSource.Part.class);

        when(datasource.openPartStream()).thenReturn(StreamResult.success(Stream.of(partStream)));
        when(partStream.openStream()).thenReturn(new ByteArrayInputStream(responseBytes));

        when(edrService.resolveByTransferProcess(transferProcessId, AUTO_REFRESH)).thenReturn(ServiceResult.success(edr()));
        when(pipelineService.transfer(any(), any()))
                .thenAnswer(a -> CompletableFuture.completedFuture(StreamResult.success(response)));

        var proxyResponseBytes = baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(ASSET_REQUEST_PATH)
                .then()
                .statusCode(200)
                .extract().body().asByteArray();

        var proxyResponse = mapper.readValue(proxyResponseBytes, new TypeReference<Map<String, String>>() {
        });

        assertThat(proxyResponse).containsAllEntriesOf(response);
    }

    @Test
    void requestAsset_shouldReturnError_whenEdrByTransferProcessIdNotFound() {

        var tp = "tp";
        var request = Map.of("transferProcessId", tp);

        when(edrService.resolveByTransferProcess(tp, AUTO_REFRESH)).thenReturn(ServiceResult.notFound("Not found"));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(ASSET_REQUEST_PATH)
                .then()
                .statusCode(400)
                .body("message", notNullValue());

    }

    @Test
    void requestAsset_shouldReturnData_withDataPlaneUrl() throws IOException {

        var transferProcessId = "tp";
        var url = "http://localhost:8080/test";
        var request = Map.of("transferProcessId", transferProcessId, PATH, "/path", QUERY_PARAMS, "test=10&foo=bar");


        var response = Map.of("response", "ok");
        var responseBytes = mapper.writeValueAsBytes(response);

        var datasource = mock(DataSource.class);
        var partStream = mock(DataSource.Part.class);

        when(datasource.openPartStream()).thenReturn(StreamResult.success(Stream.of(partStream)));
        when(partStream.openStream()).thenReturn(new ByteArrayInputStream(responseBytes));

        when(edrService.resolveByTransferProcess(transferProcessId, AUTO_REFRESH)).thenReturn(ServiceResult.success(edr(url)));
        when(pipelineService.transfer(any(), any()))
                .thenAnswer(a -> CompletableFuture.completedFuture(StreamResult.success(response)));

        var proxyResponseBytes = baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(ASSET_REQUEST_PATH)
                .then()
                .statusCode(200)
                .extract().body().asByteArray();

        var proxyResponse = mapper.readValue(proxyResponseBytes, new TypeReference<Map<String, String>>() {
        });

        assertThat(proxyResponse).containsAllEntriesOf(response);

        var captor = ArgumentCaptor.forClass(DataFlowStartMessage.class);
        verify(pipelineService).transfer(captor.capture(), any());


        var flowRequest = captor.getValue();

        assertThat(flowRequest.getSourceDataAddress().getStringProperty("baseUrl")).isEqualTo(url);

        assertThat(flowRequest.getProperties().get(QUERY_PARAMS)).isEqualTo(request.get(QUERY_PARAMS));
        assertThat(flowRequest.getProperties().get(PATH)).isEqualTo(request.get(PATH));

    }

    @Override
    protected Object controller() {
        return new ConsumerAssetRequestController(edrService, pipelineService, Executors.newSingleThreadExecutor(), mock(Monitor.class));
    }

    @Override
    protected Object additionalResource() {
        return new ClientErrorExceptionMapper();
    }

    private ArgumentMatcher<QuerySpec> queryContainsFilter(String... fields) {
        return (querySpec -> Arrays.stream(fields).allMatch(querySpec::containsAnyLeftOperand));
    }

    private DataAddress edr() {
        return edr(null);
    }

    private DataAddress edr(String baseUrl) {
        return DataAddress.Builder.newInstance().type("test").property(BASE_URL, baseUrl).build();
    }


    private EndpointDataReferenceEntry edrEntry(String assetId) {
        return edrEntry(assetId, UUID.randomUUID().toString());
    }

    private EndpointDataReferenceEntry edrEntry(String assetId, String transferProcessId, String providerId) {
        return EndpointDataReferenceEntry.Builder.newInstance()
                .assetId(assetId)
                .transferProcessId(transferProcessId)
                .contractNegotiationId(UUID.randomUUID().toString())
                .agreementId(UUID.randomUUID().toString())
                .providerId(providerId)
                .build();
    }

    private EndpointDataReferenceEntry edrEntry(String assetId, String transferProcessId) {
        return edrEntry(assetId, transferProcessId, UUID.randomUUID().toString());
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/")
                .when();
    }
}
