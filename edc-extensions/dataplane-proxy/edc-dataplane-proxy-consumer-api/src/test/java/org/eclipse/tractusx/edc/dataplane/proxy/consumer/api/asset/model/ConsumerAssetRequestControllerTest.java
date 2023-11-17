/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.specification.RequestSpecification;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset.ClientErrorExceptionMapper;
import org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset.ConsumerAssetRequestController;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiTest
public class ConsumerAssetRequestControllerTest extends RestControllerTestBase {

    public static final String ASSET_REQUEST_PATH = "/aas/request";
    private final EndpointDataReferenceCache cache = mock(EndpointDataReferenceCache.class);
    private final DataPlaneManager dataPlaneManager = mock(DataPlaneManager.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void requestAsset_shouldReturnData_withAssetId() throws IOException {

        var assetId = "assetId";
        var transferProcessId = "tp";
        var url = "http://localhost:8080/test";
        var request = Map.of("assetId", assetId, "endpointUrl", url);
        var edr = EndpointDataReference.Builder.newInstance()
                .id(transferProcessId)
                .authKey("authKey")
                .authCode("authCode")
                .contractId("contract-id")
                .endpoint(url)
                .build();

        var response = Map.of("response", "ok");
        var responseBytes = mapper.writeValueAsBytes(response);

        var datasource = mock(DataSource.class);
        var partStream = mock(DataSource.Part.class);

        when(datasource.openPartStream()).thenReturn(StreamResult.success(Stream.of(partStream)));
        when(partStream.openStream()).thenReturn(new ByteArrayInputStream(responseBytes));

        when(cache.referencesForAsset(assetId, null)).thenReturn(List.of(edr));
        when(dataPlaneManager.transfer(any()))
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
        var url = "http://localhost:8080/test";
        var request = Map.of("assetId", assetId, "endpointUrl", url);
        var edr = EndpointDataReference.Builder.newInstance()
                .id(transferProcessId)
                .authKey("authKey")
                .authCode("authCode")
                .endpoint(url)
                .contractId("contract-id")
                .build();

        when(cache.referencesForAsset(assetId, null)).thenReturn(List.of(edr));
        when(dataPlaneManager.transfer(any()))
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
        var request = Map.of("assetId", assetId, "endpointUrl", url);

        when(cache.referencesForAsset(assetId, null)).thenReturn(List.of());

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
        var url = "http://localhost:8080/test";
        var request = Map.of("assetId", assetId, "endpointUrl", url);

        var edr = EndpointDataReference.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .authKey("authKey")
                .authCode("authCode")
                .contractId("contract-id")
                .endpoint(url)
                .build();

        when(cache.referencesForAsset(assetId, null)).thenReturn(List.of(edr, edr));

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
        var url = "http://localhost:8080/test";
        var request = Map.of("transferProcessId", transferProcessId, "endpointUrl", url);
        var edr = EndpointDataReference.Builder.newInstance()
                .id(transferProcessId)
                .authKey("authKey")
                .authCode("authCode")
                .contractId("contract-id")
                .endpoint(url)
                .build();

        var response = Map.of("response", "ok");
        var responseBytes = mapper.writeValueAsBytes(response);

        var datasource = mock(DataSource.class);
        var partStream = mock(DataSource.Part.class);

        when(datasource.openPartStream()).thenReturn(StreamResult.success(Stream.of(partStream)));
        when(partStream.openStream()).thenReturn(new ByteArrayInputStream(responseBytes));

        when(cache.resolveReference(transferProcessId)).thenReturn(edr);
        when(dataPlaneManager.transfer(any()))
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
        var url = "http://localhost:8080/test";
        var request = Map.of("transferProcessId", tp, "endpointUrl", url);

        when(cache.resolveReference(tp)).thenReturn(null);

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
        var edr = EndpointDataReference.Builder.newInstance()
                .id(transferProcessId)
                .authKey("authKey")
                .authCode("authCode")
                .contractId("contract-id")
                .endpoint(url)
                .build();

        var response = Map.of("response", "ok");
        var responseBytes = mapper.writeValueAsBytes(response);

        var datasource = mock(DataSource.class);
        var partStream = mock(DataSource.Part.class);

        when(datasource.openPartStream()).thenReturn(StreamResult.success(Stream.of(partStream)));
        when(partStream.openStream()).thenReturn(new ByteArrayInputStream(responseBytes));

        when(cache.resolveReference(transferProcessId)).thenReturn(edr);
        when(dataPlaneManager.transfer(any()))
                .thenAnswer(a -> {
                    return CompletableFuture.completedFuture(StreamResult.success(response));
                });

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

        var captor = ArgumentCaptor.forClass(DataFlowRequest.class);
        verify(dataPlaneManager).transfer(captor.capture());


        var flowRequest = captor.getValue();

        assertThat(flowRequest.getSourceDataAddress().getStringProperty(BASE_URL)).isEqualTo(edr.getEndpoint());

        assertThat(flowRequest.getProperties().get(QUERY_PARAMS)).isEqualTo(request.get(QUERY_PARAMS));
        assertThat(flowRequest.getProperties().get(PATH)).isEqualTo(request.get(PATH));

    }

    @Override
    protected Object controller() {
        return new ConsumerAssetRequestController(cache, dataPlaneManager, Executors.newSingleThreadExecutor(), mock(Monitor.class));
    }

    @Override
    protected Object additionalResource() {
        return new ClientErrorExceptionMapper();
    }

    private static Stream<Arguments> provideServiceResultForProxyCall() {
        return Stream.of(
                Arguments.of(StreamResult.notFound(), NOT_FOUND.getStatusCode()),
                Arguments.of(StreamResult.notAuthorized(), UNAUTHORIZED.getStatusCode()),
                Arguments.of(StreamResult.error("error"), INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/")
                .when();
    }
}
