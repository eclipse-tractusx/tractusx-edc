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

package org.eclipse.tractusx.edc.dataplane.proxy.e2e;

import io.restassured.specification.RequestSpecification;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.tractusx.edc.dataplane.proxy.e2e.EdrCacheSetup.createEntries;
import static org.eclipse.tractusx.edc.dataplane.proxy.e2e.KeyStoreSetup.createKeyStore;
import static org.eclipse.tractusx.edc.dataplane.proxy.e2e.VaultSetup.createVaultStore;
import static org.hamcrest.Matchers.is;


/**
 * Performs end-to-end testing using a consumer data plane, a producer data plane, and a proxied HTTP endpoint.
 * <p>
 * The consumer runtime is configured with three EDRs:
 * <ul>
 * <li>One EDR is for the {@link #SINGLE_TRANSFER_ID} transfer process that is associated with a single contract agreement for the {@link #SINGLE_ASSET_ID}
 * asset</li>
 * <li>Two EDRs for transfer processes that are associated with contract agreements for the same asset, {@link #MULTI_ASSET_ID} </li>
 * </ul>
 * <p>
 * The end-to-end tests verify asset content is correctly proxied from the HTTP endpoint, error messages from the HTTP endpoint are correctly propagated,
 * and invalid requests are properly handled.
 * <p>
 * This test can be executed using the Gradle or JUnit test runners.
 */
@EndToEndTest
public class DpfProxyEndToEndTest {
    public static final String KEYSTORE_PASS = "test123";
    private static final String LAUNCHER_MODULE = ":edc-tests:edc-dataplane-proxy-e2e";
    private static final int CONSUMER_HTTP_PORT = getFreePort();
    private static final int CONSUMER_PROXY_PORT = getFreePort();
    private static final int PRODUCER_HTTP_PORT = getFreePort();
    private static final int MOCK_ENDPOINT_PORT = getFreePort();
    private static final String PROXY_SUBPATH = "proxy/aas/request";
    private static final String SINGLE_TRANSFER_ID = "5355d524-2616-43df-9096-558afffff659";
    private static final String SINGLE_ASSET_ID = "79f13b89-59a6-4278-8c8e-8540849dbab8";
    private static final String MULTI_ASSET_ID = "9260f395-3d94-4b8b-bdaa-941ead596ce5";
    private static final String REQUEST_TEMPLATE_TP = "{\"transferProcessId\": \"%s\", \"endpointUrl\" : \"http://localhost:%s/api/gateway/aas/test\"}";
    private static final String REQUEST_TEMPLATE_ASSET = "{\"assetId\": \"%s\", \"endpointUrl\" : \"http://localhost:%s/api/gateway/aas/test\"}";
    private static final String MOCK_ENDPOINT_200_BODY = "{\"message\":\"test\"}";
    @RegisterExtension
    static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            LAUNCHER_MODULE,
            "consumer",
            baseConfig(Map.of(
                    "web.http.port", valueOf(CONSUMER_HTTP_PORT),
                    "tx.dpf.consumer.proxy.port", valueOf(CONSUMER_PROXY_PORT)
            )));
    @RegisterExtension
    static EdcRuntimeExtension provider = new EdcRuntimeExtension(
            LAUNCHER_MODULE,
            "provider",
            baseConfig(Map.of(
                    "web.http.port", valueOf(PRODUCER_HTTP_PORT),
                    "tx.dpf.proxy.gateway.aas.proxied.path", "http://localhost:" + MOCK_ENDPOINT_PORT
            )));
    private MockWebServer mockEndpoint;

    private static Map<String, String> baseConfig(Map<String, String> values) {
        var map = new HashMap<>(values);
        map.put("edc.vault", createVaultStore());
        map.put("edc.keystore", createKeyStore(KEYSTORE_PASS));
        map.put("edc.keystore.password", KEYSTORE_PASS);
        return map;
    }

    @BeforeEach
    void setUp() {
        mockEndpoint = new MockWebServer();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (mockEndpoint != null) {
            mockEndpoint.shutdown();
        }
    }

    @Test
    void verify_end2EndFlows() throws IOException {

        seedEdrCache();

        // set up the HTTP endpoint
        mockEndpoint.enqueue(new MockResponse().setBody(MOCK_ENDPOINT_200_BODY));
        mockEndpoint.enqueue(new MockResponse().setBody(MOCK_ENDPOINT_200_BODY));
        mockEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockEndpoint.enqueue(new MockResponse().setResponseCode(401));
        mockEndpoint.start(MOCK_ENDPOINT_PORT);

        var tpSpec = createSpecification(format(REQUEST_TEMPLATE_TP, SINGLE_TRANSFER_ID, PRODUCER_HTTP_PORT));

        // verify content successfully proxied using a transfer process id
        tpSpec.with()
                .post(PROXY_SUBPATH)
                .then()
                .assertThat().statusCode(200)
                .assertThat().body(is(MOCK_ENDPOINT_200_BODY));

        // verify content successfully proxied using an asset id for the case where only one active transfer process exists for the asset
        var assetSpec = createSpecification(format(REQUEST_TEMPLATE_ASSET, SINGLE_ASSET_ID, PRODUCER_HTTP_PORT));
        assetSpec.with()
                .post(PROXY_SUBPATH)
                .then()
                .assertThat().statusCode(200)
                .assertThat().body(is(MOCK_ENDPOINT_200_BODY));

        // verify content not found (404) response at the endpoint is propagated
        tpSpec.with()
                .post(PROXY_SUBPATH)
                .then()
                .assertThat().statusCode(404);

        // verify unauthorized response (403) at the endpoint is propagated
        tpSpec.with()
                .post(PROXY_SUBPATH)
                .then()
                .assertThat().statusCode(401);

        // verify EDR not found results in a bad request response (400)
        var invalidSpec = createSpecification(format(REQUEST_TEMPLATE_TP, "123", PRODUCER_HTTP_PORT));
        invalidSpec.with()
                .post(PROXY_SUBPATH)
                .then()
                .assertThat().statusCode(400);

        // verify more than one contract for the same asset results in a precondition required response (428)
        var multiAssetSpec = createSpecification(format(REQUEST_TEMPLATE_ASSET, MULTI_ASSET_ID, PRODUCER_HTTP_PORT));
        multiAssetSpec.with()
                .post(PROXY_SUBPATH)
                .then()
                .assertThat().statusCode(428);
    }

    private RequestSpecification createSpecification(String body) {
        return given()
                .baseUri("http://localhost:" + CONSUMER_PROXY_PORT)
                .contentType("application/json")
                .body(body);
    }

    /**
     * Loads the EDR cache.
     */
    private void seedEdrCache() {
        var edrCache = consumer.getContext().getService(EndpointDataReferenceCache.class);
        createEntries().forEach(e -> edrCache.save(e.getEdrEntry(), e.getEdr()));
    }


}
