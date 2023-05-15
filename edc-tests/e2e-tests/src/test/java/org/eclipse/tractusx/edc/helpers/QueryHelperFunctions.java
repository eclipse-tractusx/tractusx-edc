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

import static org.eclipse.edc.api.query.QuerySpecDto.EDC_QUERY_SPEC_LIMIT;
import static org.eclipse.edc.api.query.QuerySpecDto.EDC_QUERY_SPEC_OFFSET;
import static org.eclipse.edc.api.query.QuerySpecDto.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class QueryHelperFunctions {
    
    public static JsonObject createQuery(int limit, int offset) {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_QUERY_SPEC_TYPE)
                .add(EDC_QUERY_SPEC_LIMIT, limit)
                .add(EDC_QUERY_SPEC_OFFSET, offset)
                .build();
    }
}
