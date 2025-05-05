/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.agreements.retirement.api.v3;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.eclipse.tractusx.edc.agreements.retirement.spi.service.AgreementsRetirementService;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERAND_LEFT;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERAND_RIGHT;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERATOR;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_FILTER_EXPRESSION;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore.ALREADY_EXISTS_TEMPLATE;
import static org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore.NOT_FOUND_IN_RETIREMENT_TEMPLATE;
import static org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_AGREEMENT_ID;
import static org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_REASON;
import static org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_RETIREMENT_DATE;
import static org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_TYPE;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgreementsRetirementApiV3ControllerTest extends RestControllerTestBase {

    private final AgreementsRetirementService service = mock();
    private final TypeTransformerRegistry transformer = mock();
    private final JsonObjectValidatorRegistry validator = mock();
    private final Monitor monitor = mock();

    @Test
    void should_getAllWithoutQuerySpec() {
        var agreement1 = "test-agreement-id";
        var agreement2 = "test-agreement-id2";
        var entry1 = createRetirementEntry(agreement1);
        var entry2 = createRetirementEntry(agreement2);

        when(validator.validate(any(), any()))
                .thenReturn(ValidationResult.success());
        when(transformer.transform(isA(JsonObject.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().build()));
        when(transformer.transform(isA(AgreementsRetirementEntry.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createJsonRetirementEntry(agreement1)))
                .thenReturn(Result.success(createJsonRetirementEntry(agreement2)));
        when(service.findAll(any())).thenReturn(ServiceResult.success(List.of(entry1, entry2)));

        baseRequest()
                .contentType(ContentType.JSON)
                .body("{}")
                .post("/request")
                .then()
                .log().ifError()
                .statusCode(200)
                .body(notNullValue())
                .body("size()", is(2));
    }

    @Test
    void should_getAgreementUsingFilteredQuerySpec() {
        var agreement1 = "test-agreement-id";
        var entry1 = createRetirementEntry(agreement1);
        var querySpec = QuerySpec.Builder.newInstance().filter(createFilterCriteria(agreement1)).build();

        when(validator.validate(any(), any()))
                .thenReturn(ValidationResult.success());
        when(transformer.transform(isA(JsonObject.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(querySpec));
        when(transformer.transform(isA(AgreementsRetirementEntry.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createJsonRetirementEntry(agreement1)));
        when(service.findAll(any()))
                .thenReturn(ServiceResult.success(List.of(entry1)));

        baseRequest()
                .contentType(ContentType.JSON)
                .body(createFilterQuerySpecJson(agreement1))
                .post("/request")
                .then()
                .statusCode(200)
                .body(notNullValue())
                .body("size()", is(1));
    }

    @Test
    void should_removeAgreement() {
        var agreementId = "test-agreement-id";
        when(service.reactivate(agreementId)).thenReturn(ServiceResult.success());
        baseRequest()
                .delete("/{id}", agreementId)
                .then()
                .statusCode(204);
    }

    @Test
    void shouldNot_removeAgreementWhenNotExists() {
        var agreementId = "test-agreement-id";
        when(service.reactivate(agreementId))
                .thenReturn(ServiceResult.notFound(String.format(NOT_FOUND_IN_RETIREMENT_TEMPLATE, agreementId)));

        baseRequest()
                .delete("/{id}", agreementId)
                .then()
                .log().ifError()
                .statusCode(404);
    }

    @Test
    void should_saveAgreementRetirement() {
        var agreementId = "test-agreement-id";
        when(validator.validate(any(), any()))
                .thenReturn(ValidationResult.success());
        when(transformer.transform(isA(JsonObject.class), eq(AgreementsRetirementEntry.class)))
                .thenReturn(Result.success(createRetirementEntry(agreementId)));
        when(service.retireAgreement(isA(AgreementsRetirementEntry.class)))
                .thenReturn(ServiceResult.success());

        baseRequest()
                .contentType(ContentType.JSON)
                .body(createJsonRetirementEntry(agreementId))
                .post()
                .then()
                .log().ifError()
                .statusCode(204);
    }

    @Test
    void shouldNot_saveAgreementRetirementWhenExists() {
        var agreementId = "test-agreement-id";
        when(validator.validate(any(), any()))
                .thenReturn(ValidationResult.success());
        when(transformer.transform(isA(JsonObject.class), eq(AgreementsRetirementEntry.class)))
                .thenReturn(Result.success(createRetirementEntry(agreementId)));
        when(service.retireAgreement(isA(AgreementsRetirementEntry.class)))
                .thenReturn(ServiceResult.conflict(String.format(ALREADY_EXISTS_TEMPLATE, agreementId)));

        baseRequest()
                .contentType(ContentType.JSON)
                .body(createJsonRetirementEntry(agreementId))
                .post()
                .then()
                .log().ifError()
                .statusCode(409);
    }

    @Override
    protected Object controller() {
        return new AgreementsRetirementApiV3Controller(service, transformer, validator, monitor);
    }

    private JsonObject createFilterQuerySpecJson(String agreement1) {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_QUERY_SPEC_TYPE)
                .add(EDC_QUERY_SPEC_FILTER_EXPRESSION,
                        Json.createObjectBuilder()
                                .add(CRITERION_OPERAND_LEFT, AR_ENTRY_AGREEMENT_ID)
                                .add(CRITERION_OPERATOR, "=")
                                .add(CRITERION_OPERAND_RIGHT, agreement1)
                                .build())
                .build();
    }

    private Criterion createFilterCriteria(String agreementId) {
        return Criterion.Builder.newInstance()
                .operandLeft(AR_ENTRY_AGREEMENT_ID)
                .operator("=")
                .operandRight(agreementId)
                .build();
    }

    private AgreementsRetirementEntry createRetirementEntry(String agreementId) {
        return AgreementsRetirementEntry.Builder.newInstance()
                .withAgreementId(agreementId)
                .withReason("long-reason")
                .build();
    }

    private JsonObject createJsonRetirementEntry(String agreementId) {
        return Json.createObjectBuilder()
                .add(TYPE, AR_ENTRY_TYPE)
                .add(AR_ENTRY_AGREEMENT_ID, agreementId)
                .add(AR_ENTRY_REASON, "long-reason")
                .add(AR_ENTRY_RETIREMENT_DATE, Instant.now().toString())
                .build();
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/v3/contractagreements/retirements")
                .when();
    }


}