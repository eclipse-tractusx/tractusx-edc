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

package org.eclipse.tractusx.edc.api.edr.transform;

import jakarta.json.Json;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_AGREEMENT_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_ASSET_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_CONTRACT_NEGOTIATION_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_PROVIDER_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_TRANSFER_PROCESS_ID;
import static org.mockito.Mockito.mock;

class JsonObjectFromEndpointDataReferenceEntryTransformerTest {

    private final TransformerContext context = mock(TransformerContext.class);
    private JsonObjectFromEndpointDataReferenceEntryTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromEndpointDataReferenceEntryTransformer(Json.createBuilderFactory(Map.of()));
    }

    @Test
    void transform() {

        var dto = EndpointDataReferenceEntry.Builder.newInstance()
                .assetId("id")
                .transferProcessId("tpId")
                .agreementId("aId")
                .providerId("providerId")
                .contractNegotiationId("contractNegotiationId")
                .build();

        var jsonObject = transformer.transform(dto, context);

        assertThat(jsonObject).isNotNull();
        assertThat(jsonObject.getJsonString(EDR_ENTRY_AGREEMENT_ID).getString()).isNotNull().isEqualTo(dto.getAgreementId());
        assertThat(jsonObject.getJsonString(EDR_ENTRY_CONTRACT_NEGOTIATION_ID).getString()).isNotNull().isEqualTo(dto.getContractNegotiationId());
        assertThat(jsonObject.getJsonString(EDR_ENTRY_ASSET_ID).getString()).isNotNull().isEqualTo(dto.getAssetId());
        assertThat(jsonObject.getJsonString(EDR_ENTRY_TRANSFER_PROCESS_ID).getString()).isNotNull().isEqualTo(dto.getTransferProcessId());
        assertThat(jsonObject.getJsonString(EDR_ENTRY_PROVIDER_ID).getString()).isNotNull().isEqualTo(dto.getProviderId());

    }
}
