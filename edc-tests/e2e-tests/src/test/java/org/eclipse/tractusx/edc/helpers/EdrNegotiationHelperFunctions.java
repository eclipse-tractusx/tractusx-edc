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
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.api.edr.dto.NegotiateEdrRequestDto;

import java.util.Set;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.mockito.Mockito.mock;

public class EdrNegotiationHelperFunctions {

    private static final JsonLd JSON_LD = new TitaniumJsonLd(mock(Monitor.class));

    public static JsonObject createEdrNegotiationRequest(String connectorAddress, String providerId, String offerId, String assetId, JsonObject policy, JsonArray callbacks) {
        return Json.createObjectBuilder()
                .add(TYPE, NegotiateEdrRequestDto.EDR_REQUEST_DTO_TYPE)
                .add(EDC_NAMESPACE + "connectorId", providerId)
                .add(EDC_NAMESPACE + "providerId", providerId)
                .add(EDC_NAMESPACE + "connectorAddress", connectorAddress)
                .add(EDC_NAMESPACE + "protocol", DATASPACE_PROTOCOL_HTTP)
                .add(EDC_NAMESPACE + "offer", Json.createObjectBuilder()
                        .add(EDC_NAMESPACE + "offerId", offerId)
                        .add(EDC_NAMESPACE + "assetId", assetId)
                        .add(EDC_NAMESPACE + "policy", JSON_LD.compact(policy).getContent())
                )
                .add(EDC_NAMESPACE + "callbackAddresses", callbacks)
                .build();
    }

    public static JsonObject createCallback(String url, boolean transactional, Set<String> events) {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "CallbackAddress")
                .add(EDC_NAMESPACE + "transactional", transactional)
                .add(EDC_NAMESPACE + "uri", url)
                .add(EDC_NAMESPACE + "events", events
                        .stream()
                        .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                        .build())
                .build();
    }

    public static <E extends Event> ReceivedEvent createEvent(Class<E> klass) {
        return ReceivedEvent.Builder.newInstance().type(klass.getSimpleName()).build();
    }
}
