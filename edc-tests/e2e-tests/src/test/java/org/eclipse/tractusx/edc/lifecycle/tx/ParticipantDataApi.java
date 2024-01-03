/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.lifecycle.tx;

import io.restassured.response.Response;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.lifecycle.tx.TxParticipant.API_KEY;
import static org.eclipse.tractusx.edc.lifecycle.tx.TxParticipant.PROXY_SUBPATH;


/**
 * E2E test helper for fetching the data
 */
public class ParticipantDataApi {

    private final TxParticipant participant;

    public ParticipantDataApi(TxParticipant participant) {
        this.participant = participant;
    }
    
    /**
     * Pull the data using the Consumer DataPlane and the provider's gateway with an asset id
     *
     * @return the data
     */
    public String pullProxyDataByAssetId(TxParticipant provider, String assetId) {
        var body = Map.of("assetId", assetId, "endpointUrl", format("%s/gateway/aas/test", provider.gatewayEndpoint()));
        return getProxyData(body);
    }

    /**
     * Pull the data using the Consumer DataPlane and the provider's gateway with an transfer process id
     *
     * @return the data
     */
    public String pullProxyDataByTransferProcessId(TxParticipant provider, String transferProcessId) {
        var body = Map.of("transferProcessId", transferProcessId,
                "endpointUrl", format("%s/gateway/aas/test", provider.gatewayEndpoint()));
        return getProxyData(body);

    }

    /**
     * Pull the data using the Consumer DataPlane and the provider's gateway with an asset id
     *
     * @return the data
     */
    public Response pullProxyDataResponseByAssetId(TxParticipant provider, String assetId) {
        var body = Map.of("assetId", assetId,
                "endpointUrl", format("%s/gateway/aas/test", provider.gatewayEndpoint()),
                "providerId", provider.getBpn());
        return proxyRequest(body);
    }

    /**
     * Pull the data using the Consumer DataPlane with an asset id
     *
     * @return the data
     */
    public String pullProviderDataPlaneDataByAssetId(TxParticipant provider, String assetId) {
        var body = Map.of("assetId", assetId);
        return getProxyData(body);
    }

    /**
     * Pull the data using the Consumer DataPlane with an asset id and additional parameters
     *
     * @return the data
     */
    public String pullProviderDataPlaneDataByAssetIdAndCustomProperties(TxParticipant provider, String assetId, String path, String params) {
        var body = Map.of("assetId", assetId, "pathSegments", path, "queryParams", params);
        return getProxyData(body);
    }

    /**
     * Pull the data using the Consumer DataPlane
     *
     * @return the data
     */
    public String pullProviderDataPlaneDataByTransferProcessId(TxParticipant provider, String transferProcessId) {
        var body = Map.of("transferProcessId", transferProcessId);
        return getProxyData(body);

    }

    /**
     * Pull the data with an {@link EndpointDataReference}
     *
     * @param edr         The edr
     * @param queryParams additional params
     * @return the data
     */
    public String pullData(EndpointDataReference edr, Map<String, String> queryParams) {
        var response = given()
                .baseUri(edr.getEndpoint())
                .header(edr.getAuthKey(), edr.getAuthCode())
                .queryParams(queryParams)
                .when()
                .get();
        assertThat(response.statusCode()).isBetween(200, 300);
        return response.body().asString();
    }

    private String getProxyData(Map<String, String> body) {
        return proxyRequest(body)
                .then()
                .assertThat().statusCode(200)
                .extract().body().asString();
    }

    private Response proxyRequest(Map<String, String> body) {
        return given()
                .baseUri(participant.dataPlaneProxy().toString())
                .header("x-api-key", API_KEY)
                .contentType("application/json")
                .body(body)
                .post(PROXY_SUBPATH);
    }
}
