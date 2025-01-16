/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.framework;

import org.eclipse.edc.connector.dataplane.spi.Endpoint;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;


class PublicEndpointGeneratorServiceImplTest {

    private final PublicEndpointGeneratorService generatorService = new PublicEndpointGeneratorServiceImpl();

    @Test
    void supportedTypes() {
        generatorService.addGeneratorFunction("type", dataAddress -> new Endpoint("any", "any"));

        var result = generatorService.supportedDestinationTypes();

        assertThat(result).containsOnly("type");
    }

    @Nested
    class GenerateFor {

        @Test
        void shouldGenerateEndpointBasedOnDestinationType() {
            var endpoint = new Endpoint("fizz", "bar-type");

            generatorService.addGeneratorFunction("destinationType", dataAddress -> endpoint);
            var sourceAddress = DataAddress.Builder.newInstance().type("testtype").build();

            var result = generatorService.generateFor("destinationType", sourceAddress);

            assertThat(result).isSucceeded().isEqualTo(endpoint);
        }

        @Test
        void shouldFail_whenFunctionIsNotRegistered() {
            var sourceAddress = DataAddress.Builder.newInstance().type("testtype").build();

            var result = generatorService.generateFor("any", sourceAddress);

            assertThat(result).isFailed();
        }

    }

    @Nested
    class GenerateResponseFor {

        @Test
        void shouldGenerateEndpointBasedOnDestinationType() {
            var endpoint = new Endpoint("fizz", "bar-type");

            generatorService.addGeneratorFunction("destinationType", () -> endpoint);

            var result = generatorService.generateResponseFor("destinationType");

            assertThat(result).isSucceeded().isEqualTo(endpoint);
        }

        @Test
        void shouldFail_whenFunctionIsNotRegistered() {
            var result = generatorService.generateResponseFor("any");
            assertThat(result).isFailed();
        }

    }
}