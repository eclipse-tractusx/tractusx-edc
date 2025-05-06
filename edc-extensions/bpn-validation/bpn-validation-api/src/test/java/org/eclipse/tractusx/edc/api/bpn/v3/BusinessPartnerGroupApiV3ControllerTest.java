/*
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
 */

package org.eclipse.tractusx.edc.api.bpn.v3;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.tractusx.edc.api.bpn.BaseBusinessPartnerGroupApiControllerTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ApiTest
class BusinessPartnerGroupApiV3ControllerTest extends BaseBusinessPartnerGroupApiControllerTest {

    @Test
    void resolveForBpnGroup_exists() {
        when(businessPartnerStore.resolveForBpnGroup(any())).thenReturn(StoreResult.success(List.of("bpn1", "bpn2")));
        baseRequest()
                .get("/group/test-bpn-group")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    void resolveForBpnGroup_notExists_returns404() {
        when(businessPartnerStore.resolveForBpnGroup(any())).thenReturn(StoreResult.notFound("test-message"));
        baseRequest()
                .get("/group/test-bpn-group-not-exists")
                .then()
                .statusCode(404);
    }

    @Test
    void resolveForBpnGroups_exists() {
        when(businessPartnerStore.resolveForBpnGroups()).thenReturn(StoreResult.success(List.of("group1", "group2")));
        baseRequest()
                .get("/groups")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    void resolveForBpnGroups_notExists_returns404() {
        when(businessPartnerStore.resolveForBpnGroups()).thenReturn(StoreResult.notFound("test-message"));
        baseRequest()
                .get("/groups")
                .then()
                .statusCode(404);
    }

    @Override
    protected Object controller() {
        return new BusinessPartnerGroupApiV3Controller(businessPartnerStore);
    }

    @Override
    protected RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/v3/business-partner-groups")
                .when();
    }

}
