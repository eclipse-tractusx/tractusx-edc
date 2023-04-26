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
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.CoreConstants.EDC_PREFIX;

public class AssetHelperFunctions {

    /**
     * Creates an asset with the given ID and props using the participant's Data Management API
     */
    public static JsonObject createAsset(String id, JsonObject assetProperties, JsonObject dataAddress) {
        return Json.createObjectBuilder()
                .add(CONTEXT, createContextBuilder())
                .add(TYPE, EDC_NAMESPACE + "AssetEntryDto")
                .add(EDC_NAMESPACE + "asset", Json.createObjectBuilder()
                        .add(ID, id)
                        .add(EDC_NAMESPACE + "properties", assetProperties)
                        .build())
                .add(EDC_NAMESPACE + "dataAddress", dataAddress)
                .build();


    }

    public static JsonObjectBuilder createDataAddressBuilder(String type) {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", type);
    }

    public static JsonObjectBuilder createContextBuilder() {
        return Json.createObjectBuilder()
                .add(EDC_PREFIX, EDC_NAMESPACE);
    }

}
