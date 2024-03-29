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

package org.eclipse.tractusx.edc.tests;

import jakarta.json.JsonObject;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * E2E test helper for fetching the data
 */
public class ParticipantDataApi {

    /**
     * Pull the data with an {@link DataAddress}
     *
     * @param edr         The edr
     * @param queryParams additional params
     * @return the data
     */
    public String pullData(JsonObject edr, Map<String, String> queryParams) {

        var endpoint = edr.getString("endpoint");
        var token = edr.getString("authorization");
        var response = given()
                .baseUri(endpoint)
                .header("Authorization", token)
                .queryParams(queryParams)
                .when()
                .get();
        assertThat(response.statusCode()).isBetween(200, 300);
        return response.body().asString();
    }

}
