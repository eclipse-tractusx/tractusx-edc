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

package org.eclipse.tractusx.edc.validators.emptyassetselector;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import org.assertj.core.api.Assertions;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.ValidationFailure;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ACCESSPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ASSETS_SELECTOR;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_CONTRACTPOLICY_ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERAND_LEFT;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERAND_RIGHT;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERATOR;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmptyAssetSelectorValidatorTest {

    CriterionOperatorRegistry criterionOperatorRegistry = mock();

    private final JsonObjectValidator validator = EmptyAssetSelectorValidator.instance(criterionOperatorRegistry);

    @Test
    void shouldPass_whenContractDefinitionIsCorrect() {

        var criterion = createObjectBuilder()
                .add(CRITERION_OPERAND_LEFT, value("operandLeft"))
                .add(CRITERION_OPERATOR, value("="))
                .add(CRITERION_OPERAND_RIGHT, value("operandRight"));

        var contractDefinition = createObjectBuilder()
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, value("accessPolicyId"))
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, value("accessPolicyId"))
                .add(CONTRACT_DEFINITION_ASSETS_SELECTOR, createArrayBuilder().add(criterion))
                .build();

        when(criterionOperatorRegistry.isSupported("=")).thenReturn(true);

        var result = validator.validate(contractDefinition);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenContractDefinitionHasNoAssetSelector() {
        var contractDefinition = createObjectBuilder()
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, value("accessPolicyId"))
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, value("accessPolicyId"))
                .build();

        var result = validator.validate(contractDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> Assertions.assertThat(violation.path()).isEqualTo(CONTRACT_DEFINITION_ASSETS_SELECTOR))
                .anySatisfy(violation -> Assertions.assertThat(violation.message()).contains("is missing"));
    }

    @Test
    void shouldFail_whenContractDefinitionHasEmptyAssetSelector() {
        var contractDefinition = createObjectBuilder()
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, value("accessPolicyId"))
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, value("accessPolicyId"))
                .add(CONTRACT_DEFINITION_ASSETS_SELECTOR, Json.createArrayBuilder().build())
                .build();

        var result = validator.validate(contractDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> Assertions.assertThat(violation.path()).isEqualTo(CONTRACT_DEFINITION_ASSETS_SELECTOR))
                .anySatisfy(violation -> Assertions.assertThat(violation.message()).contains("should at least contains '1' elements"));
    }

    /**
     * Set of tests from upstream ContractDefinitionValidatorTest that still need to pass here.
     **/

    @Test
    void shouldFail_whenMandatoryFieldsAreMissing() {
        var contractDefinition = createObjectBuilder().build();

        var result = validator.validate(contractDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> Assertions.assertThat(violation.path()).isEqualTo(CONTRACT_DEFINITION_ACCESSPOLICY_ID))
                .anySatisfy(violation -> Assertions.assertThat(violation.path()).isEqualTo(CONTRACT_DEFINITION_CONTRACTPOLICY_ID));
    }

    @Test
    void shouldFail_whenIdIsBlank() {
        var contractDefinition = createObjectBuilder()
                .add(ID, " ")
                .build();

        var result = validator.validate(contractDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .filteredOn(it -> ID.equals(it.path()))
                .anySatisfy(violation -> Assertions.assertThat(violation.message()).contains("blank"));
    }

    @Test
    void shouldFail_whenAccessPolicyIdIsBlank() {
        var contractDefinition = createObjectBuilder()
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, value(" "))
                .build();

        var result = validator.validate(contractDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .filteredOn(it -> CONTRACT_DEFINITION_ACCESSPOLICY_ID.equals(it.path()))
                .anySatisfy(violation -> Assertions.assertThat(violation.message()).contains("blank"));
    }

    @Test
    void shouldFail_whenContractPolicyIdIsBlank() {
        var contractDefinition = createObjectBuilder()
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, value(" "))
                .build();

        var result = validator.validate(contractDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .filteredOn(it -> CONTRACT_DEFINITION_CONTRACTPOLICY_ID.equals(it.path()))
                .anySatisfy(violation -> Assertions.assertThat(violation.message()).contains("blank"));
    }

    @Test
    void shouldFail_whenAssetSelectorCriterionIsNotValid() {
        var contractDefinition = createObjectBuilder()
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, value("id"))
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, value("id"))
                .add(CONTRACT_DEFINITION_ASSETS_SELECTOR, createArrayBuilder().add(createObjectBuilder()
                        .add(CRITERION_OPERAND_LEFT, value(" "))
                        .add(CRITERION_OPERATOR, value(" "))
                ))
                .build();

        var result = validator.validate(contractDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> Assertions.assertThat(violation.path()).endsWith(CRITERION_OPERAND_LEFT))
                .anySatisfy(violation -> Assertions.assertThat(violation.path()).endsWith(CRITERION_OPERATOR));
    }

    private JsonArrayBuilder value(String value) {
        return createArrayBuilder().add(createObjectBuilder().add(VALUE, value));
    }
}