/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.dataplane.kafka.validator;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.validator.spi.ValidationFailure;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.BOOTSTRAP_SERVERS;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.MECHANISM;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.OAUTH_CLIENT_ID;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.OAUTH_CLIENT_SECRET_KEY;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.OAUTH_TOKEN_URL;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.PROTOCOL;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.TOPIC;

class KafkaBrokerDataAddressValidatorTest {

    private final KafkaBrokerDataAddressValidator validator = new KafkaBrokerDataAddressValidator();

    @Test
    void shouldPass_whenDataAddressIsValid() {
        var dataAddress = DataAddress.Builder.newInstance()
                .type("Kafka")
                .property(TOPIC, "topic.name")
                .property(BOOTSTRAP_SERVERS, "any:98123")
                .property(MECHANISM, "OAUTHBEARER")
                .property(PROTOCOL, "SASL_PLAINTEXT")
                .property(OAUTH_TOKEN_URL, "http://keycloak/token")
                .property(OAUTH_CLIENT_ID, "client-id")
                .property(OAUTH_CLIENT_SECRET_KEY, "clientSecretKey")
                .build();

        var result = validator.validate(dataAddress);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenRequiredFieldsAreMissing() {
        var dataAddress = DataAddress.Builder.newInstance()
                .type("Kafka")
                .build();

        var result = validator.validate(dataAddress);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations)
                .satisfies(violations -> assertThat(violations).extracting(Violation::path)
                        .containsExactlyInAnyOrder(TOPIC, BOOTSTRAP_SERVERS, MECHANISM, PROTOCOL, OAUTH_TOKEN_URL,
                                OAUTH_CLIENT_ID, OAUTH_CLIENT_SECRET_KEY));
    }
}
