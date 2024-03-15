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

package org.eclipse.tractusx.edc.api.edr.v2.transform;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_AGREEMENT_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_ASSET_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_CONTRACT_NEGOTIATION_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_EXPIRATION_DATE;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_PROVIDER_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_STATE;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_TRANSFER_PROCESS_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_TYPE;


public class JsonObjectFromEndpointDataReferenceEntryTransformer extends AbstractJsonLdTransformer<EndpointDataReferenceEntry, JsonObject> {

    public JsonObjectFromEndpointDataReferenceEntryTransformer() {
        super(EndpointDataReferenceEntry.class, JsonObject.class);
    }

    @Override
    public @Nullable JsonObject transform(@NotNull EndpointDataReferenceEntry dto, @NotNull TransformerContext context) {

        var builder = Json.createObjectBuilder()
                .add(TYPE, EDR_ENTRY_TYPE)
                .add(EDR_ENTRY_AGREEMENT_ID, dto.getAgreementId())
                .add(EDR_ENTRY_TRANSFER_PROCESS_ID, dto.getTransferProcessId())
                .add(EDR_ENTRY_ASSET_ID, dto.getAssetId())
                .add(EDR_ENTRY_STATE, dto.getEdrState())
                .add(EDR_ENTRY_EXPIRATION_DATE, dto.getExpirationTimestamp());

        if (dto.getProviderId() != null) {
            builder.add(EDR_ENTRY_PROVIDER_ID, dto.getProviderId());
        }

        if (dto.getContractNegotiationId() != null) {
            builder.add(EDR_ENTRY_CONTRACT_NEGOTIATION_ID, dto.getContractNegotiationId());
        }

        return builder.build();
    }


}
