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

package org.eclipse.edc.connector.controlplane.transform.edc.catalog.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_ADDITIONAL_SCOPES;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ID;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_PROTOCOL;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_QUERY_SPEC;

public class JsonObjectToCatalogRequestTransformer extends AbstractJsonLdTransformer<JsonObject, CatalogRequest> {

    public JsonObjectToCatalogRequestTransformer() {
        super(JsonObject.class, CatalogRequest.class);
    }

    @Override
    public @Nullable CatalogRequest transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var counterPartyAddress = transformString(object.get(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS), context);

        // For backward compatibility if the ID is not sent, fallback to the counterPartyAddress
        var counterPartyId = ofNullable(object.get(CATALOG_REQUEST_COUNTER_PARTY_ID))
                .map(it -> transformString(it, context))
                .orElse(counterPartyAddress);

        var querySpec = ofNullable(object.get(CATALOG_REQUEST_QUERY_SPEC))
                .map(it -> transformObject(it, QuerySpec.class, context))
                .orElse(null);

        var builder = CatalogRequest.Builder.newInstance()
                .protocol(transformString(object.get(CATALOG_REQUEST_PROTOCOL), context))
                .counterPartyAddress(counterPartyAddress)
                .counterPartyId(counterPartyId)
                .querySpec(querySpec);


        ofNullable(object.getJsonArray(CATALOG_REQUEST_ADDITIONAL_SCOPES))
                .ifPresent(ja -> builder.additionalScopes(ja.stream().map(this::nodeValue).toList()));

        return builder.build();
    }

}
