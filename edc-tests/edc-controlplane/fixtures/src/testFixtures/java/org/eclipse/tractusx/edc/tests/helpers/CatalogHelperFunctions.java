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

package org.eclipse.tractusx.edc.tests.helpers;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_QUERY_SPEC;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_ATTRIBUTE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class CatalogHelperFunctions {


    public static JsonObject createCatalogRequest(JsonObject query, String dspEndpoint) {
        var jsonBuilder = Json.createObjectBuilder();
        jsonBuilder.add("@type", "CatalogRequest");
        jsonBuilder.add(EDC_NAMESPACE + "counterPartyAddress", dspEndpoint);
        jsonBuilder.add(EDC_NAMESPACE + "protocol", "dataspace-protocol-http");

        if (query != null) {
            jsonBuilder.add(CATALOG_REQUEST_QUERY_SPEC, query);
        }
        return jsonBuilder.build();
    }

    public static String getDatasetOfferId(JsonObject dataset) {
        return dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject().getString(ID);
    }

    public static String getDatasetAssetId(JsonObject dataset) {
        return dataset.getString(ID);
    }

    public static JsonArray getDatasetPolicies(JsonObject dataset) {
        return dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE);
    }

    public static JsonObject getDatasetFirstPolicy(JsonObject dataset) {
        return dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).stream().findFirst().get().asJsonObject();
    }

    public static JsonObject getDatasetFirstPolicy(JsonValue dataset) {
        return getDatasetFirstPolicy(dataset.asJsonObject());
    }

    public static JsonArray getDatasetPolicies(JsonValue dataset) {
        return getDatasetPolicies(dataset.asJsonObject());
    }
}
