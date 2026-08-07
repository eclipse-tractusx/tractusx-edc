/********************************************************************************
 * Copyright (c) 2026 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import jakarta.json.JsonObject;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ACCESSPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_CONTRACTPOLICY_ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;
import static org.hamcrest.Matchers.equalTo;

@EndToEndTest
public class ContractDefinitionPoliciesValidatorsTest {

    private static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_DID)
            .bpn(PROVIDER_BPN)
            .build();

    @RegisterExtension
    @Order(0)
    private static final PostgresExtension POSTGRES = new PostgresExtension(PROVIDER.getName());

    @RegisterExtension
    private static final RuntimeExtension PROVIDER_RUNTIME = pgRuntime(PROVIDER, POSTGRES);

    @Test
    void shouldFail_whenAccessPolicyDefinedAsContractPolicy() {
        var contractPolicyId = PROVIDER.createPolicyDefinition(contractPolicy());

        var contractDefinition = contractDefinition("contract-definition", contractPolicyId, contractPolicyId);
        PROVIDER.baseManagementRequest()
                .contentType(JSON)
                .body(contractDefinition)
                .when()
                .post("/v3/contractdefinitions")
                .then().assertThat()
                .log().ifValidationFails()
                .statusCode(400)
                .body("[0].message", equalTo(
                        "Policy '%s' does not have the expected permission action 'https://w3id.org/catenax/2025/9/policy/access'"
                                .formatted(contractPolicyId)));
    }

    @Test
    void shouldFail_whenContractPolicyDefinedAsAccessPolicy() {
        var accessPolicyId = PROVIDER.createPolicyDefinition(accessPolicy());

        var contractDefinition = contractDefinition("contract-definition", accessPolicyId, accessPolicyId);
        PROVIDER.baseManagementRequest()
                .contentType(JSON)
                .body(contractDefinition)
                .when()
                .post("/v3/contractdefinitions")
                .then().assertThat()
                .statusCode(400)
                .body("[0].message",
                        equalTo("Policy '%s' does not have the expected permission action 'http://www.w3.org/ns/odrl/2/use'"
                                .formatted(accessPolicyId)));
    }

    @Test
    void shouldPass_whenContractDefinitionHasCorrectPolicies() {
        var accessPolicyId = PROVIDER.createPolicyDefinition(accessPolicy());
        var contractPolicyId = PROVIDER.createPolicyDefinition(contractPolicy());

        PROVIDER.createContractDefinition("assetId", "contract-definition", accessPolicyId, contractPolicyId);
    }

    @Test
    void shouldFail_whenUpdatingPolicyThatIsReferencedByContractDefinition() {
        var accessPolicyId = PROVIDER.createPolicyDefinition(accessPolicy());
        var contractPolicyId = PROVIDER.createPolicyDefinition(contractPolicy());
        PROVIDER.createContractDefinition("assetId", "contract-definition", accessPolicyId, contractPolicyId);

        var policyDefinition = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                .add(TYPE, "PolicyDefinition")
                .add(ID, contractPolicyId)
                .add("policy", accessPolicy())
                .build();

        PROVIDER.baseManagementRequest()
                .contentType(JSON)
                .body(policyDefinition)
                .when()
                .put("/v3/policydefinitions/" + contractPolicyId)
                .then().assertThat()
                .statusCode(400)
                .body("[0].message", equalTo("Policy Definition is referenced by a Contract Definition"));
    }

    private JsonObject accessPolicy() {
        return createObjectBuilder()
                .add("@context", createArrayBuilder()
                        .add("https://w3id.org/dspace/2025/1/odrl-profile.jsonld")
                        .add("https://w3id.org/catenax/2025/9/policy/context.jsonld"))
                .add("@type", "Set")
                .add("permission", createObjectBuilder()
                        .add("action", "access")
                        .add("constraint", createObjectBuilder()
                                .add("leftOperand", "Membership")
                                .add("operator", "eq")
                                .add("rightOperand", "active")))
                .build();
    }

    private JsonObject contractPolicy() {
        return createObjectBuilder()
                .add("@context", createArrayBuilder()
                        .add("https://w3id.org/dspace/2025/1/odrl-profile.jsonld")
                        .add("https://w3id.org/catenax/2025/9/policy/context.jsonld"))
                .add("@type", "Set")
                .add("permission", createObjectBuilder()
                        .add("action", "use")
                        .add("constraint", createObjectBuilder()
                                .add("and", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("leftOperand", "UsagePurpose")
                                                .add("operator", "isAnyOf")
                                                .add("rightOperand", createArrayBuilder()
                                                        .add("cx.core.digitalTwinRegistry:1")))
                                        .add(createObjectBuilder()
                                                .add("leftOperand", "FrameworkAgreement")
                                                .add("operator", "eq")
                                                .add("rightOperand", "DataExchangeGovernance:1.0")))))
                .build();
    }

    private JsonObject contractDefinition(String id, String accessPolicyId, String contractPolicyId) {
        return createObjectBuilder()
                .add(CONTEXT, createObjectBuilder())
                .add(ID, id)
                .add(TYPE, EDC_NAMESPACE + "ContractDefinition")
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, accessPolicyId)
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, contractPolicyId)
                .add("assetsSelector", createArrayBuilder().add(createObjectBuilder()
                        .add("@type", "CriterionDto")
                        .add("operandLeft", "https://w3id.org/edc/v0.0.1/ns/id")
                        .add("operator", "=")
                        .add("operandRight", "asset")))
                .build();
    }
}
