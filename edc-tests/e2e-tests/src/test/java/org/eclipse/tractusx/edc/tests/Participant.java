package org.eclipse.tractusx.edc.tests;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.api.management.catalog.model.CatalogRequestDto;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.types.TypeManager;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class Participant extends EdcRuntimeExtension {

    private final String managementUrl;
    private final String apiKey;
    private final String idsEndpoint;
    private final TypeManager typeManager = new TypeManager();
    private final String idsId;

    public Participant(String moduleName, String runtimeName, Map<String, String> properties) {
        super(moduleName, runtimeName, properties);
        this.managementUrl = URI.create(format("http://localhost:%s%s", properties.get("web.http.management.port"), properties.get("web.http.management.path"))).toString();
        this.idsEndpoint = URI.create(format("http://localhost:%s%s", properties.get("web.http.ids.port"), properties.get("web.http.ids.path"))).toString();
        this.apiKey = properties.get("edc.api.auth.key");
        this.idsId = properties.get("edc.ids.id");
    }

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


    public void createPolicy(PolicyDefinition policyDefinition) {
        baseRequest()
                .body(policyDefinition)
                .when()
                .post("/policydefinitions")
                .then()
                .statusCode(200)
                .contentType(JSON).contentType(JSON);
    }

    public Catalog requestCatalog(Participant other) {

        var response = baseRequest()
                .when()
                .body(CatalogRequestDto.Builder.newInstance().providerUrl(other.idsEndpoint()+"/data").build())
                .post("/catalog/request")
                .then();

        var code = response.extract().statusCode();
        var body = response.extract().body().asString();

        // doing an assertJ style assertion will allow us to use the body as fail message if the return code != 200
        assertThat(code).withFailMessage(body).isEqualTo(200);
        return typeManager.readValue(body, Catalog.class);
    }

    public String idsId() {
        return idsId;
    }

    private String idsEndpoint() {
        return idsEndpoint;
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri(managementUrl)
                .header("x-api-key", apiKey)
                .contentType(JSON);
    }
}
