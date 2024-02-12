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

package org.eclipse.tractusx.edc.edr.core.defaults;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.junit.jupiter.api.Test;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

class PersistentCacheEntryTest {

    @Test
    void verify_serializeDeserialize() throws JsonProcessingException {
        var mapper = new ObjectMapper();

        var edr = EndpointDataReference.Builder.newInstance()
                .endpoint("http://test.com")
                .id(randomUUID().toString())
                .authCode("11111")
                .contractId("test-contract-id")
                .authKey("authentication").build();

        var edrEntry = EndpointDataReferenceEntry.Builder.newInstance()
                .assetId(randomUUID().toString())
                .agreementId(randomUUID().toString())
                .transferProcessId(randomUUID().toString())
                .providerId(randomUUID().toString())
                .build();

        var serialized = mapper.writeValueAsString(new PersistentCacheEntry(edrEntry, edr));

        var deserialized = mapper.readValue(serialized, PersistentCacheEntry.class);

        assertThat(deserialized.getEdrEntry()).isNotNull();
        assertThat(deserialized.getEdr()).isNotNull();
    }


}
