/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.lifecycle;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.api.management.catalog.model.CatalogRequestDto;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.InjectionContainer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.token.MockDapsService;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class Participant extends EdcRuntimeExtension implements BeforeAllCallback, AfterAllCallback {

    private final String managementUrl;
    private final String apiKey;
    private final String idsEndpoint;
    private final TypeManager typeManager = new TypeManager();
    private final String idsId;
    private DataWiper wiper;
    private final String bpn;

    public Participant(String moduleName, String runtimeName, Map<String, String> properties) {
        super(moduleName, runtimeName, properties);
        this.managementUrl = URI.create(format("http://localhost:%s%s", properties.get("web.http.management.port"), properties.get("web.http.management.path"))).toString();
        this.idsEndpoint = URI.create(format("http://localhost:%s%s", properties.get("web.http.ids.port"), properties.get("web.http.ids.path"))).toString();
        this.apiKey = properties.get("edc.api.auth.key");
        this.idsId = properties.get("edc.ids.id");
        this.bpn = runtimeName + "-BPN";
        this.registerServiceMock(IdentityService.class, new MockDapsService(getBpn()));
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        //do nothing - we only want to start the runtime once
        wiper.clearPersistence();
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
    }

    @Override
    protected void bootExtensions(ServiceExtensionContext context, List<InjectionContainer<ServiceExtension>> serviceExtensions) {
        super.bootExtensions(context, serviceExtensions);
        wiper = new DataWiper(context);
    }

    /**
     * Creates an asset with the given ID and props using the participant's Data Management API
     */
    public void createAsset(String id, Map<String, String> properties) {
        properties = new HashMap<>(properties);
        properties.put("asset:prop:id", id);
        properties.put("asset:prop:description", "test description");

        var asset = Map.of(
                "asset", Map.of(
                        "id", id,
                        "properties", properties
                ),
                "dataAddress", Map.of(
                        "properties", Map.of("type", "test-type")

                )
        );

        baseRequest()
                .body(asset)
                .when()
                .post("/assets")
                .then()
                .statusCode(200)
                .contentType(JSON);

    }

    /**
     * Creates a {@link org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition} using the participant's Data Management API
     */
    public void createContractDefinition(String assetId, String definitionId, String accessPolicyId, String contractPolicyId, long contractValidityDurationSeconds) {
        var contractDefinition = Map.of(
                "id", definitionId,
                "accessPolicyId", accessPolicyId,
                "validity", String.valueOf(contractValidityDurationSeconds),
                "contractPolicyId", contractPolicyId,
                "criteria", AssetSelectorExpression.Builder.newInstance().constraint("asset:prop:id", "=", assetId).build().getCriteria()
        );

        baseRequest()
                .body(contractDefinition)
                .when()
                .post("/contractdefinitions")
                .then()
                .statusCode(200)
                .contentType(JSON).contentType(JSON);
    }

    /**
     * Creates a {@link PolicyDefinition} using the participant's Data Management API
     */
    public void createPolicy(PolicyDefinition policyDefinition) {
        baseRequest()
                .body(policyDefinition)
                .when()
                .post("/policydefinitions")
                .then()
                .statusCode(200)
                .contentType(JSON).contentType(JSON);
    }

    /**
     * Requests the {@link Catalog} from another participant using this participant's Data Management API
     */
    public Catalog requestCatalog(Participant other) {
        return requestCatalog(other, QuerySpecDto.Builder.newInstance().build());
    }

    /**
     * Requests the {@link Catalog} from another participant using this participant's Data Management API
     */
    public Catalog requestCatalog(Participant other, QuerySpecDto query) {
        var response = baseRequest()
                .when()
                .body(CatalogRequestDto.Builder.newInstance()
                        .providerUrl(other.idsEndpoint + "/data")
                        .querySpec(query)
                        .build())
                .post("/catalog/request")
                .then();

        var code = response.extract().statusCode();
        var body = response.extract().body().asString();

        // doing an assertJ style assertion will allow us to use the body as fail message if the return code != 200
        assertThat(code).withFailMessage(body).isEqualTo(200);
        return typeManager.readValue(body, Catalog.class);
    }

    /**
     * Returns this participant's IDS ID
     */
    public String idsId() {
        return idsId;
    }

    /**
     * Returns this participant's BusinessPartnerNumber (=BPN). This is constructed of the runtime name plus "-BPN"
     */
    public String getBpn() {
        return bpn;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        //only run this once
        super.beforeTestExecution(context);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        super.afterTestExecution(context);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri(managementUrl)
                .header("x-api-key", apiKey)
                .contentType(JSON);
    }
}
