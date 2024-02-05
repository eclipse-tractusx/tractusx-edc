/********************************************************************************
 * Copyright (c) 2024 Robert Bosch Manufacturing Solutions GmbH and others
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.client.model;

import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DtrAccessVerificationRequestTest {

    private final TypeManager typeManager = new TypeManager();

    @Test
    void test_JsonDeserialization_ShouldGetBackOriginalRequest_WhenCalledWithSerializedValidData() {
        //given
        final var request = new DtrAccessVerificationRequest("http://edc-data-plane/url");
        final var asString = typeManager.writeValueAsString(request);

        //when
        final var actual = typeManager.readValue(asString, DtrAccessVerificationRequest.class);

        //then
        assertThat(actual).isEqualTo(request);
    }

    @Test
    void test_JsonSerialization_ShouldProduceExpectedJsonFormat_WhenCalledWithValidData() {
        //given
        final var request = new DtrAccessVerificationRequest("http://edc-data-plane/url");

        //when
        final var actual = typeManager.writeValueAsString(request);

        //then
        assertThat(actual).isEqualTo("{\"submodelEndpointUrl\":\"http://edc-data-plane/url\"}");
    }
}