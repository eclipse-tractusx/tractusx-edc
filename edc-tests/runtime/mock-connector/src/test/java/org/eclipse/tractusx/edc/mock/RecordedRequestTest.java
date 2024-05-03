/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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

package org.eclipse.tractusx.edc.mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecordedRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        var module = new SimpleModule();
        module.addDeserializer(ServiceFailure.class, new ServiceFailureDeserializer());
        mapper.registerModule(module);
    }

    @Test
    void verifySerDes() throws JsonProcessingException {
        var json = TestUtils.getResourceFileContentAsString("asset.creation.json");
        var rr = mapper.readValue(json, RecordedRequest.class);

        assertThat(rr).isNotNull();
        assertThat(rr.getInput()).isInstanceOf(Asset.class);
        assertThat(rr.getOutput()).isInstanceOf(Asset.class);
        assertThat(rr.getInputMatchType()).isEqualTo(MatchType.CLASS);
        assertThat(rr.getFailure()).isNull();
        assertThat(rr.getName()).isEqualTo("Asset Creation V3");
        assertThat(rr.getDescription()).isEqualTo("test description");
    }

    @Test
    void verifySerDes_withFailure() throws JsonProcessingException {
        var json = TestUtils.getResourceFileContentAsString("asset.failure.json");
        var rr = mapper.readValue(json, RecordedRequest.class);

        assertThat(rr).isNotNull();
        assertThat(rr.getInput()).isInstanceOf(Asset.class);
        assertThat(rr.getOutput()).isNull();
        assertThat(rr.getInputMatchType()).isEqualTo(MatchType.CLASS);
        assertThat(rr.getName()).isEqualTo("Some failed asset request");
        assertThat(rr.getDescription()).isEqualTo("test description");
        assertThat(rr.getFailure()).isInstanceOf(ServiceFailure.class);
        assertThat(rr.getFailure().getReason()).isEqualTo(ServiceFailure.Reason.UNAUTHORIZED);
        assertThat(rr.getFailure().getMessages()).containsExactlyInAnyOrder("message 1", "message 2");
    }
}