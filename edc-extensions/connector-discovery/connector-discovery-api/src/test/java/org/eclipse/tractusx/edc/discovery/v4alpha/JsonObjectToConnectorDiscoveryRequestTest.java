/*
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
import org.eclipse.tractusx.edc.discovery.v4alpha.transformers.JsonObjectToConnectorDiscoveryRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryRequest.CONNECTOR_DISCOVERY_REQUEST_COUNTERPARTYID_ATTRIBUTE;
import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryRequest.CONNECTOR_DISCOVERY_REQUEST_KNOWNCONNECTORS_ATTRIBUTE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JsonObjectToConnectorDiscoveryRequestTest {

    private final TransformerContext transformerContext = mock();
    private final JsonObjectToConnectorDiscoveryRequest transformer = new JsonObjectToConnectorDiscoveryRequest();

    @Test
    void testTransform() {
        var jsonObject = Json.createObjectBuilder()
                .add(CONNECTOR_DISCOVERY_REQUEST_COUNTERPARTYID_ATTRIBUTE, "testIdentifier")
                .add(CONNECTOR_DISCOVERY_REQUEST_KNOWNCONNECTORS_ATTRIBUTE,
                        Json.createArrayBuilder().add("testKnownAddress"))
                .build();

        var request = transformer.transform(jsonObject, transformerContext);

        assertThat(request).isNotNull();
        assertThat(request.counterPartyId()).isEqualTo("testIdentifier");
        assertThat(request.knownConnectors().size()).isEqualTo(1);
        assertThat(request.knownConnectors().iterator().next()).isEqualTo("testKnownAddress");
    }

    @Test
    void testTransformNoKnowns() {
        var jsonObject = Json.createObjectBuilder()
                .add(CONNECTOR_DISCOVERY_REQUEST_COUNTERPARTYID_ATTRIBUTE, "testIdentifier")
                .build();

        var request = transformer.transform(jsonObject, transformerContext);

        assertThat(request).isNotNull();
        assertThat(request.counterPartyId()).isEqualTo("testIdentifier");
        assertThat(request.knownConnectors()).isNull();
    }

    @Test
    void testTransformEmptyKnowns() {
        var jsonObject = Json.createObjectBuilder()
                .add(CONNECTOR_DISCOVERY_REQUEST_COUNTERPARTYID_ATTRIBUTE, "testIdentifier")
                .add(CONNECTOR_DISCOVERY_REQUEST_KNOWNCONNECTORS_ATTRIBUTE, Json.createArrayBuilder())
                .build();

        var request = transformer.transform(jsonObject, transformerContext);

        assertThat(request).isNotNull();
        assertThat(request.counterPartyId()).isEqualTo("testIdentifier");
        assertThat(request.knownConnectors()).isNull();
    }

    @Test
    void testTransformMissingAttribute() {
        var jsonObject = Json.createObjectBuilder()
                .add(CONNECTOR_DISCOVERY_REQUEST_KNOWNCONNECTORS_ATTRIBUTE, Json.createArrayBuilder().add("testKnownAddress"))
                .build();

        var request = transformer.transform(jsonObject, transformerContext);

        assertThat(request).isNull();
        verify(transformerContext).reportProblem("Missing required attribute in ConnectorDiscoveryRequest: tx:counterPartyId");
    }
}
