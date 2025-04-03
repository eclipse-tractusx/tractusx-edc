/********************************************************************************
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.tests.validators;

import io.restassured.response.ValidatableResponse;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ACCESSPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ASSETS_SELECTOR;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_CONTRACTPOLICY_ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;
import static org.hamcrest.Matchers.contains;

@EndToEndTest
public class EmptyAssetSelectorValidatorTest {

    private static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_BPN)
            .build();

    @RegisterExtension
    @Order(0)
    private static final PostgresExtension POSTGRES = new PostgresExtension(PROVIDER.getName());

    @RegisterExtension
    protected static final RuntimeExtension PROVIDER_RUNTIME = pgRuntime(PROVIDER, POSTGRES,
            () -> PROVIDER.getConfig().merge(ConfigFactory.fromMap(
                    Map.of("tx.edc.validator.contractdefinitions.block-empty-asset-selector", "true"))
            )
    );

    @Test
    @DisplayName("Provider gets 400 when no asset selector is used")
    void shouldFail_whenContractDefinitionHasNoAssetSelector() {

        var requestResponse = createContractDefinitionRequest("definitionId", "accessPolicyId", "contractPolicy", null);

        requestResponse.statusCode(400)
                .body("message", contains("mandatory array '%s' is missing".formatted(CONTRACT_DEFINITION_ASSETS_SELECTOR)));

    }

    @Test
    @DisplayName("Provider gets 400 when empty asset selector is used")
    void shouldFail_whenContractDefinitionHasEmptyAssetSelector() {

        var requestResponse = createContractDefinitionRequest("definitionId", "accessPolicyId", "contractPolicy", createArrayBuilder().build());

        requestResponse.statusCode(400)
                .body("message", contains("array '%s' should at least contains '1' elements".formatted(CONTRACT_DEFINITION_ASSETS_SELECTOR)));
    }

    @Test
    @DisplayName("Provider gets 200 when asset selector has a valid criterion")
    void shouldPass_whenContractDefinitionHasCorrectAssetSelector() {
        var assetSelector = Json.createArrayBuilder()
                .add(createObjectBuilder()
                        .add(TYPE, "Criterion")
                        .add(EDC_NAMESPACE + "operandLeft", EDC_NAMESPACE + "id")
                        .add(EDC_NAMESPACE + "operator", "=")
                        .add(EDC_NAMESPACE + "operandRight", "assetId")
                        .build())
                .build();

        var requestResponse = createContractDefinitionRequest("definitionId", "accessPolicyId", "contractPolicy", assetSelector);

        requestResponse.statusCode(200);
    }

    private ValidatableResponse createContractDefinitionRequest(String definitionId, String accessPolicyId, String contractPolicyId, JsonArray criterionArray) {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(ID, definitionId)
                .add(TYPE, EDC_NAMESPACE + "ContractDefinition")
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, accessPolicyId)
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, contractPolicyId);

        if (criterionArray != null) {
            requestBody.add(CONTRACT_DEFINITION_ASSETS_SELECTOR, criterionArray);
        }

        return PROVIDER.baseManagementRequest()
                .contentType(JSON)
                .body(requestBody.build())
                .when()
                .post("/v3/contractdefinitions")
                .then();
    }

}
