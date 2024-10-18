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

import io.restassured.http.ContentType;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participant;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * E2E test helper for fetching the data
 */
public class ParticipantConsumerDataPlaneApi {


    private final Participant.Endpoint dataPlaneProxy;

    public ParticipantConsumerDataPlaneApi(Participant.Endpoint dataPlaneProxy) {

        this.dataPlaneProxy = dataPlaneProxy;
    }


    public String pullData(Map<String, String> body) {
        var response = dataPlaneProxy.baseRequest()
                .body(body)
                .contentType(ContentType.JSON)
                .post("/proxy/aas/request");

        assertThat(response.statusCode()).isBetween(200, 300);
        return response.body().asString();
    }

}
