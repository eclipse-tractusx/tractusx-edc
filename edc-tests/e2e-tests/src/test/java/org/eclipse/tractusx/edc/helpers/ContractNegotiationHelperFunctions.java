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
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.mockito.Mockito.mock;

public class ContractNegotiationHelperFunctions {

    private final static JsonLd jsonLd = new TitaniumJsonLd(mock(Monitor.class));

    public static JsonObject createNegotiationRequest(String connectorAddress, String providerId, String offerId, String assetId, JsonObject policy) {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "NegotiationInitiateRequestDto")
                .add(EDC_NAMESPACE + "connectorId", providerId)
                .add(EDC_NAMESPACE + "providerId", providerId)
                .add(EDC_NAMESPACE + "connectorAddress", connectorAddress)
                .add(EDC_NAMESPACE + "protocol", DATASPACE_PROTOCOL_HTTP)
                .add(EDC_NAMESPACE + "offer", Json.createObjectBuilder()
                        .add(EDC_NAMESPACE + "offerId", offerId)
                        .add(EDC_NAMESPACE + "assetId", assetId)
                        .add(EDC_NAMESPACE + "policy", jsonLd.compact(policy).getContent())
                )
                .build();
    }

}
