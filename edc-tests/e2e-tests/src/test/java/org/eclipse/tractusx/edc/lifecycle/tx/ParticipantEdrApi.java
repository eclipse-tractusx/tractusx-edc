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

package org.eclipse.tractusx.edc.lifecycle.tx;

import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;

/**
 * E2E test helper for the EDR APIs
 */
public class ParticipantEdrApi {

    private final URI edrBackend;

    public ParticipantEdrApi(URI edrBackend) {
        this.edrBackend = edrBackend;
    }

    /**
     * Get the cached EDR for a transfer process cached in a backend
     *
     * @param transferProcessId The transfer process id
     * @return The EDR
     */
    public EndpointDataReference getDataReferenceFromBackend(String transferProcessId) {
        var dataReference = new AtomicReference<EndpointDataReference>();

        var result = given()
                .when()
                .get(edrBackend + "/{id}", transferProcessId)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(EndpointDataReference.class);
        dataReference.set(result);

        return dataReference.get();
    }

}
