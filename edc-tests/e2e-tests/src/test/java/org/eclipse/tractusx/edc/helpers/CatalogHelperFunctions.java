/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.helpers;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.contract.spi.ContractId;

import static org.eclipse.edc.catalog.spi.CatalogRequest.EDC_CATALOG_REQUEST_QUERY_SPEC;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_ATTRIBUTE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

public class CatalogHelperFunctions {


    public static JsonObject createCatalogRequest(JsonObject query, String dspEndpoint) {
        var jsonBuilder = Json.createObjectBuilder();
        jsonBuilder.add("@type", "CatalogRequest");
        jsonBuilder.add(EDC_NAMESPACE + "providerUrl", dspEndpoint);
        jsonBuilder.add(EDC_NAMESPACE + "protocol", "dataspace-protocol-http");

        if (query != null) {
            jsonBuilder.add(EDC_CATALOG_REQUEST_QUERY_SPEC, query);
        }
        return jsonBuilder.build();
    }

    public static ContractId getDatasetContractId(JsonObject dataset) {
        var id = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject().getString(ID);
        return ContractId.parse(id);
    }

    public static String getDatasetAssetId(JsonObject dataset) {
        return getDatasetContractId(dataset).assetIdPart();
    }

    public static String getDatasetAssetId(JsonValue dataset) {
        return getDatasetContractId(dataset.asJsonObject()).assetIdPart();
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
