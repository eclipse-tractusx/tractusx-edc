/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.edr.spi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.junit.jupiter.api.Test;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

class EndpointDataReferenceEntryTest {

    @Test
    void verify_serializeDeserialize() throws JsonProcessingException {
        var mapper = new ObjectMapper();

        var entry = EndpointDataReferenceEntry.Builder.newInstance()
                .assetId(randomUUID().toString())
                .agreementId(randomUUID().toString())
                .transferProcessId(randomUUID().toString())
                .build();

        var serialized = mapper.writeValueAsString(entry);
        var deserialized = mapper.readValue(serialized, EndpointDataReferenceEntry.class);

        assertThat(deserialized.getTransferProcessId()).isNotEmpty();
        assertThat(deserialized.getAssetId()).isNotEmpty();
        assertThat(deserialized.getAgreementId()).isNotEmpty();
    }
}
