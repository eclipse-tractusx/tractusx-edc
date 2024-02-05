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

package org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HttpAccessControlErrorApiControllerTest {

    @Test
    void test_Unauthorized_ShouldReturnHttp401_WhenCalledWithReasonCode() {
        //given
        final String aReason = "a reason";
        final var underTest = new HttpAccessControlErrorApiController();

        //when
        final Response actual = underTest.unauthorized(aReason);

        //then
        assertThat(actual).isNotNull();
        assertThat(actual.getStatus()).isEqualTo(401);
    }

    @Test
    void test_Forbidden_ShouldReturnHttp403_WhenCalledWithReasonCode() {
        //given
        final String aReason = "a reason";
        final var underTest = new HttpAccessControlErrorApiController();

        //when
        final Response actual = underTest.forbidden(aReason);

        //then
        assertThat(actual).isNotNull();
        assertThat(actual.getStatus()).isEqualTo(403);
    }

    @Test
    void test_Ok_ShouldReturnHttp200_WhenCalledWithBody() {
        //given
        final String aBody = "{\"a\":\"b\"}";
        final var underTest = new HttpAccessControlErrorApiController();

        //when
        final Response actual = underTest.ok(aBody);

        //then
        assertThat(actual).isNotNull();
        assertThat(actual.getStatus()).isEqualTo(200);
    }
}