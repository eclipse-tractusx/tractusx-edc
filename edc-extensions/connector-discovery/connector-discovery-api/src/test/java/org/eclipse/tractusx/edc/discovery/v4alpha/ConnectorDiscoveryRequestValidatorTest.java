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
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.tractusx.edc.discovery.v4alpha.validators.ConnectorDiscoveryRequestValidator;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryRequest.CONNECTOR_DISCOVERY_REQUEST_COUNTERPARTYID_ATTRIBUTE;
import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryRequest.CONNECTOR_DISCOVERY_REQUEST_KNOWNCONNECTORS_ATTRIBUTE;

public class ConnectorDiscoveryRequestValidatorTest {

    private final Validator<JsonObject> validator = ConnectorDiscoveryRequestValidator.instance();

    @Test
    void shouldSucceed_whenRequestUsesBpnlAndNoKnowns() {
        JsonObject validRequest = Json.createObjectBuilder()
                .add(CONNECTOR_DISCOVERY_REQUEST_COUNTERPARTYID_ATTRIBUTE, value("BPNL1234567890"))
                .build();

        var result = validator.validate(validRequest);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldSucceed_whenRequestUsesDidAndEmptyKnowns() {
        JsonObject validRequest = Json.createObjectBuilder()
                .add(CONNECTOR_DISCOVERY_REQUEST_COUNTERPARTYID_ATTRIBUTE, value("did:web:example.com"))
                .add(CONNECTOR_DISCOVERY_REQUEST_KNOWNCONNECTORS_ATTRIBUTE, arrayValue(Collections.emptyList()))
                .build();

        var result = validator.validate(validRequest);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldSucceed_whenRequestUsesDidAndKnowns() {
        JsonObject validRequest = Json.createObjectBuilder()
                .add(CONNECTOR_DISCOVERY_REQUEST_COUNTERPARTYID_ATTRIBUTE, value("did:web:example.com"))
                .add(CONNECTOR_DISCOVERY_REQUEST_KNOWNCONNECTORS_ATTRIBUTE,
                        arrayValue(List.of("https://example.com/c1/api/v1/dsp", "https://example.com/c2/api/v1/dsp")))
                .build();

        var result = validator.validate(validRequest);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldSucceed_whenRequestHasOtherProps() {
        JsonObject validRequest = Json.createObjectBuilder()
                .add(CONNECTOR_DISCOVERY_REQUEST_COUNTERPARTYID_ATTRIBUTE, value("did:web:example.com"))
                .add(CONNECTOR_DISCOVERY_REQUEST_KNOWNCONNECTORS_ATTRIBUTE,
                        arrayValue(List.of("https://example.com/c1/api/v1/dsp", "https://example.com/c2/api/v1/dsp")))
                .add("additionalProperty", "someValue")
                .build();

        var result = validator.validate(validRequest);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenIdentifierIsMissing() {
        JsonObject invalidRequest = Json.createObjectBuilder()
                .add(CONNECTOR_DISCOVERY_REQUEST_KNOWNCONNECTORS_ATTRIBUTE,
                        arrayValue(List.of("https://example.com/c1/api/v1/dsp", "https://example.com/c2/api/v1/dsp")))
                .build();

        var result = validator.validate(invalidRequest);

        assertThat(result).isFailed();
    }

    @Test
    void shouldFail_whenKnownsIsNotArray() {
        JsonObject invalidRequest = Json.createObjectBuilder()
                .add(CONNECTOR_DISCOVERY_REQUEST_COUNTERPARTYID_ATTRIBUTE, value("did:web:example.com"))
                .add(CONNECTOR_DISCOVERY_REQUEST_KNOWNCONNECTORS_ATTRIBUTE, value("https://example.com/c1/api/v1/dsp"))
                .build();

        var result = validator.validate(invalidRequest);

        assertThat(result).isFailed();
        assertThat(result.getFailureDetail()).contains("is not of type STRING");
    }

    @Test
    void shouldFail_whenKnownsContainsNonUrls() {
        JsonObject invalidRequest = Json.createObjectBuilder()
                .add(CONNECTOR_DISCOVERY_REQUEST_COUNTERPARTYID_ATTRIBUTE, value("did:web:example.com"))
                .add(CONNECTOR_DISCOVERY_REQUEST_KNOWNCONNECTORS_ATTRIBUTE,
                        arrayValue(List.of("foo", "bar")))
                .build();

        var result = validator.validate(invalidRequest);

        assertThat(result).isFailed();
        assertThat(result.getFailureDetail()).contains("foo").contains("bar");
    }

    @Test
    void shouldFail_whenKnownsContainsNonStrings() {
        JsonObject invalidRequest = createObjectBuilder()
                .add(CONNECTOR_DISCOVERY_REQUEST_COUNTERPARTYID_ATTRIBUTE, value("did:web:example.com"))
                .add(CONNECTOR_DISCOVERY_REQUEST_KNOWNCONNECTORS_ATTRIBUTE, createArrayBuilder()
                        .add(3)
                        .add(createObjectBuilder().add("key", "value")))
                .build();

        var result = validator.validate(invalidRequest);

        assertThat(result).isFailed();
        assertThat(result.getFailureDetail()).contains("3").contains("key").contains("value");
    }

    private JsonArrayBuilder value(String value) {
        return createArrayBuilder().add(createObjectBuilder().add(VALUE, value));
    }

    private JsonArrayBuilder arrayValue(Collection<String> values) {
        JsonArrayBuilder builder = createArrayBuilder();
        for (String value : values) {
            builder.add(value);
        }
        return builder;
    }
}
