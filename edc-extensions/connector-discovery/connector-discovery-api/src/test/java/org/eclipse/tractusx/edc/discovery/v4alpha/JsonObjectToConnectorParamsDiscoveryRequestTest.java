/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2026 Cofinity-X GmbH
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
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest.DISCOVERY_PARAMS_REQUEST_COUNTER_PARTY_ADDRESS_ATTRIBUTE;
import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest.DISCOVERY_PARAMS_REQUEST_IDENTIFIER_ATTRIBUTE;
import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest.DISCOVERY_PARAMS_REQUEST_IDENTIFIER_ATTRIBUTE_LEGACY;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JsonObjectToConnectorParamsDiscoveryRequestTest {

    private final TransformerContext transformerContext = mock();
    private final JsonObjectToConnectorParamsDiscoveryRequest transformer = new JsonObjectToConnectorParamsDiscoveryRequest();

    @ParameterizedTest
    @ArgumentsSource(JsonObjectToConnectorParamsDiscoveryRequestTest.RequestProvider.class)
    void testTransform(String id, String bpnl, String counterPartyAddress, String expectedIdentifier) {
        var jsonObject = Json.createObjectBuilder()
                .add(DISCOVERY_PARAMS_REQUEST_COUNTER_PARTY_ADDRESS_ATTRIBUTE, counterPartyAddress);
        if (id != null) {
            jsonObject.add(DISCOVERY_PARAMS_REQUEST_IDENTIFIER_ATTRIBUTE, id);
        }
        if (bpnl != null) {
            jsonObject.add(DISCOVERY_PARAMS_REQUEST_IDENTIFIER_ATTRIBUTE_LEGACY, bpnl);
        }

        var request = transformer.transform(jsonObject.build(), transformerContext);

        assertThat(request).isNotNull();
        assertThat(request.identifier()).isEqualTo(expectedIdentifier);
        assertThat(request.counterPartyAddress()).isEqualTo(counterPartyAddress);
    }

    private static class RequestProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    of("BPNL1234567890AB", null, "https://provider.domain.com/api/dsp", "BPNL1234567890AB"),
                    of("did:web:example.com", null, "https://provider.domain.com/api/dsp", "did:web:example.com"),
                    of(null, "BPNL1234567890AB", "https://provider.domain.com/api/dsp", "BPNL1234567890AB"),
                    of("did:web:example.com", "BPNL1234567890AB", "https://provider.domain.com/api/dsp", "did:web:example.com")
            );
        }
    }

    @Test
    void testTransformMissingIdAttribute() {
        var jsonObject = Json.createObjectBuilder()
                .add(DISCOVERY_PARAMS_REQUEST_COUNTER_PARTY_ADDRESS_ATTRIBUTE, "testCounterPartyAddress")
                .build();

        var request = transformer.transform(jsonObject, transformerContext);

        assertThat(request).isNull();
        verify(transformerContext).reportProblem("Missing required attributes in ConnectorParamsDiscoveryRequest: edc:counterPartyId or edc:counterPartyAddress");
    }


    @Test
    void testTransformMissingAddressAttribute() {
        var jsonObject = Json.createObjectBuilder()
                .add(DISCOVERY_PARAMS_REQUEST_IDENTIFIER_ATTRIBUTE, "testCounterPartyId")
                .build();

        var request = transformer.transform(jsonObject, transformerContext);

        assertThat(request).isNull();
        verify(transformerContext).reportProblem("Missing required attributes in ConnectorParamsDiscoveryRequest: edc:counterPartyId or edc:counterPartyAddress");
    }
}