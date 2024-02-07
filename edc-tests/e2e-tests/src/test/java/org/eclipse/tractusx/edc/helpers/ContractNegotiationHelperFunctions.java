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

    private static final JsonLd JSON_LD = new TitaniumJsonLd(mock(Monitor.class));

    public static JsonObject createNegotiationRequest(String counterPartyAddress, String providerId, JsonObject policy) {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "NegotiationInitiateRequestDto")
                .add(EDC_NAMESPACE + "providerId", providerId)
                .add(EDC_NAMESPACE + "counterPartyAddress", counterPartyAddress)
                .add(EDC_NAMESPACE + "protocol", DATASPACE_PROTOCOL_HTTP)
                .add(EDC_NAMESPACE + "policy", JSON_LD.compact(policy).getContent())
                .build();
    }

}
