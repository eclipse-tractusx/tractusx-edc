/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.discovery.v4alpha;

import jakarta.json.Json;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.discovery.v4alpha.transformers.JsonObjectToConnectorParamsDiscoveryRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest.DISCOVERY_PARAMS_REQUEST_BPNL_ATTRIBUTE;
import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest.DISCOVERY_PARAMS_REQUEST_COUNTER_PARTY_ADDRESS_ATTRIBUTE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JsonObjectToConnectorParamsDiscoveryRequestTest {

    private final TransformerContext transformerContext = mock();
    private final JsonObjectToConnectorParamsDiscoveryRequest transformer = new JsonObjectToConnectorParamsDiscoveryRequest();

    @Test
    void testTransform() {
        var jsonObject = Json.createObjectBuilder()
                .add(DISCOVERY_PARAMS_REQUEST_BPNL_ATTRIBUTE, "testBPNL")
                .add(DISCOVERY_PARAMS_REQUEST_COUNTER_PARTY_ADDRESS_ATTRIBUTE, "testCounterPartyAddress")
                .build();

        var request = transformer.transform(jsonObject, transformerContext);

        assertThat(request).isNotNull();
        assertThat(request.bpnl()).isEqualTo("testBPNL");
        assertThat(request.counterPartyAddress()).isEqualTo("testCounterPartyAddress");
    }

    @Test
    void testTransformMissingAttribute() {
        var jsonObject = Json.createObjectBuilder()
                .add(DISCOVERY_PARAMS_REQUEST_COUNTER_PARTY_ADDRESS_ATTRIBUTE, "testCounterPartyAddress")
                .build();

        var request = transformer.transform(jsonObject, transformerContext);

        assertThat(request).isNull();
        verify(transformerContext).reportProblem("Missing required attributes in ConnectorParamsDiscoveryRequest: tx:bpnl or edc:counterPartyAddress");
    }

}