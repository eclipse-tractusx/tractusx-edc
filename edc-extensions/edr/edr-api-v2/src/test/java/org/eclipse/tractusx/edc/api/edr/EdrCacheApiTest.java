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

package org.eclipse.tractusx.edc.api.edr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_AGREEMENT_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_ASSET_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_CONTRACT_NEGOTIATION_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_CREATED_AT;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_PROVIDER_ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.tractusx.edc.api.edr.v2.EdrCacheApiV2.EndpointDataReferenceEntrySchema.EDR_ENTRY_OUTPUT_EXAMPLE;
import static org.mockito.Mockito.mock;

public class EdrCacheApiTest {

    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final JsonLd jsonLd = new TitaniumJsonLd(mock());

    @Test
    void edrEntryOutputExample() throws JsonProcessingException {
        var jsonObject = objectMapper.readValue(EDR_ENTRY_OUTPUT_EXAMPLE, JsonObject.class);
        var expanded = jsonLd.expand(jsonObject);

        assertThat(expanded).isSucceeded().satisfies(content -> {
            assertThat(content.getString(ID)).isNotBlank();
            assertThat(content.getJsonArray(EDR_ENTRY_AGREEMENT_ID).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(EDR_ENTRY_CONTRACT_NEGOTIATION_ID).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(EDR_ENTRY_PROVIDER_ID).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(EDR_ENTRY_ASSET_ID).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(EDR_ENTRY_CREATED_AT).getJsonObject(0).getJsonNumber(VALUE)).isNotNull();
        });
    }
}
