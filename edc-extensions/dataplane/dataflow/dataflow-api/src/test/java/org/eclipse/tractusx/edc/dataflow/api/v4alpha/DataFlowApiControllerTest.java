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

package org.eclipse.tractusx.edc.dataflow.api.v4alpha;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.eclipse.tractusx.edc.spi.dataflow.DataFlowService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ApiTest
class DataFlowApiControllerTest extends RestControllerTestBase {

    private static final String DATAFLOW_ID = "123";

    private final DataFlowService service = mock();

    @Override
    protected Object controller() {
        return new DataFlowApiController(monitor, service);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/v4alpha/dataflows");
    }

    @Nested
    class Trigger {

        @Test
        void triggerDataTransfer_shouldReturnNotFound_whenServiceReturnsNotFound() {
            when(service.trigger(DATAFLOW_ID)).thenReturn(ServiceResult.notFound("not-found"));

            baseRequest()
                    .when()
                    .contentType(JSON)
                    .post("/{id}/trigger", DATAFLOW_ID)
                    .then()
                    .statusCode(404);
        }

        @Test
        void triggerDataTransfer_shouldReturnBadRequest_whenServiceReturnsBadRequest() {
            when(service.trigger(DATAFLOW_ID)).thenReturn(ServiceResult.badRequest("bad-request"));

            baseRequest()
                    .when()
                    .contentType(JSON)
                    .post("/{id}/trigger", DATAFLOW_ID)
                    .then()
                    .statusCode(400);
        }

        @Test
        void triggerDataTransfer_shouldReturnConflict_whenServiceReturnsConflict() {
            when(service.trigger(DATAFLOW_ID)).thenReturn(ServiceResult.conflict("conflict"));

            baseRequest()
                    .when()
                    .contentType(JSON)
                    .post("/{id}/trigger", DATAFLOW_ID)
                    .then()
                    .statusCode(409);
        }

        @Test
        void triggerDataTransfer_shouldReturnNoContent_whenServiceReturnsSuccess() {
            when(service.trigger(DATAFLOW_ID)).thenReturn(ServiceResult.success());

            baseRequest()
                    .when()
                    .contentType(JSON)
                    .post("/{id}/trigger", DATAFLOW_ID)
                    .then()
                    .log().ifError()
                    .statusCode(204);
        }

    }

}
