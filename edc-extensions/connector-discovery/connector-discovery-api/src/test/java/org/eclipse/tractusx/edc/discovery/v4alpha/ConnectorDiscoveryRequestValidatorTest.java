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
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.tractusx.edc.discovery.v4alpha.validators.ConnectorDiscoveryRequestValidator;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest.DISCOVERY_PARAMS_REQUEST_BPNL_ATTRIBUTE;
import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest.DISCOVERY_PARAMS_REQUEST_COUNTER_PARTY_ADDRESS_ATTRIBUTE;

class ConnectorDiscoveryRequestValidatorTest {

    private final Validator<JsonObject> validator = ConnectorDiscoveryRequestValidator.instance();

    @Test
    void shouldSucceed_whenRequestIsValid() {
        JsonObject validRequest = Json.createObjectBuilder()
                .add(DISCOVERY_PARAMS_REQUEST_BPNL_ATTRIBUTE, value("BPNL1234567890"))
                .add(DISCOVERY_PARAMS_REQUEST_COUNTER_PARTY_ADDRESS_ATTRIBUTE, value("https://provider.domain.com/api/dsp"))
                .build();

        var result = validator.validate(validRequest);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldSucceed_whenRequestHasOtherProps() {
        JsonObject validRequest = Json.createObjectBuilder()
                .add(DISCOVERY_PARAMS_REQUEST_BPNL_ATTRIBUTE, value("BPNL1234567890"))
                .add(DISCOVERY_PARAMS_REQUEST_COUNTER_PARTY_ADDRESS_ATTRIBUTE, value("https://provider.domain.com/api/dsp"))
                .add("additionalProperty", value("someValue"))
                .build();

        var result = validator.validate(validRequest);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenBpnlIsMissing() {
        JsonObject validRequest = Json.createObjectBuilder()
                .add(DISCOVERY_PARAMS_REQUEST_COUNTER_PARTY_ADDRESS_ATTRIBUTE, value("https://provider.domain.com/api/dsp"))
                .build();

        var result = validator.validate(validRequest);

        assertThat(result).isFailed();
    }

    @Test
    void shouldFail_whenCounterPartyAddressIsMissing() {
        JsonObject validRequest = Json.createObjectBuilder()
                .add(DISCOVERY_PARAMS_REQUEST_BPNL_ATTRIBUTE, value("BPNL1234567890"))
                .build();

        var result = validator.validate(validRequest);

        assertThat(result).isFailed();
    }

    private JsonArrayBuilder value(String value) {
        return createArrayBuilder().add(createObjectBuilder().add(VALUE, value));
    }

}