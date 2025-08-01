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

package org.eclipse.tractusx.edc.api.bpn.v1;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.tractusx.edc.api.bpn.BaseBusinessPartnerGroupApiControllerTest;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.mock;

@ApiTest
class BusinessPartnerGroupApiV1ControllerTest extends BaseBusinessPartnerGroupApiControllerTest {

    @Override
    protected Object controller() {
        return new BusinessPartnerGroupApiV1Controller(businessPartnerStore, mock(), mock());
    }

    @Override
    protected RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/business-partner-groups")
                .when();
    }

}
