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

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_AGREEMENT_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_ASSET_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_CONTRACT_NEGOTIATION_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_CREATED_AT;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_PROVIDER_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_TRANSFER_PROCESS_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromEndpointDataReferenceEntryTransformer extends AbstractJsonLdTransformer<EndpointDataReferenceEntry, JsonObject> {
    private final JsonBuilderFactory jsonFactory;


    public JsonObjectFromEndpointDataReferenceEntryTransformer(JsonBuilderFactory jsonFactory) {
        super(EndpointDataReferenceEntry.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull EndpointDataReferenceEntry entry, @NotNull TransformerContext context) {
        return jsonFactory.createObjectBuilder()
                .add(ID, entry.getId())
                .add(TYPE, EDR_ENTRY_TYPE)
                .add(EDR_ENTRY_PROVIDER_ID, entry.getProviderId())
                .add(EDR_ENTRY_ASSET_ID, entry.getAssetId())
                .add(EDR_ENTRY_AGREEMENT_ID, entry.getAgreementId())
                .add(EDR_ENTRY_TRANSFER_PROCESS_ID, entry.getTransferProcessId())
                .add(EDR_ENTRY_CREATED_AT, entry.getCreatedAt())
                .add(EDR_ENTRY_CONTRACT_NEGOTIATION_ID, entry.getContractNegotiationId())
                .build();
    }
}
