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
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

class PublicEndpointGeneratorServiceImpl implements PublicEndpointGeneratorService {
    private final Map<String, Function<DataAddress, Endpoint>> generatorFunctions = new ConcurrentHashMap<>();
    private final Map<String, Supplier<Endpoint>> responseChannelFunctions = new ConcurrentHashMap<>();

    @Override
    public Result<Endpoint> generateFor(String destinationType, DataAddress sourceDataAddress) {
        var function = generatorFunctions.get(destinationType);
        if (function == null) {
            return Result.failure("No Endpoint generator function registered for transfer type destination '%s'".formatted(destinationType));
        }

        var endpoint = function.apply(sourceDataAddress);
        return Result.success(endpoint);
    }

    @Override
    public Result<Endpoint> generateResponseFor(String responseChannelType) {
        var function = responseChannelFunctions.get(responseChannelType);
        return ofNullable(function).map(Supplier::get).map(Result::success)
                .orElseGet(() -> Result.failure("No Response Channel Endpoint generator function registered for response channel type '%s'".formatted(responseChannelType)));
    }

    @Override
    public void addGeneratorFunction(String destinationType, Function<DataAddress, Endpoint> generatorFunction) {
        generatorFunctions.put(destinationType, generatorFunction);
    }

    @Override
    public void addGeneratorFunction(String responseChannelType, Supplier<Endpoint> generatorFunction) {
        responseChannelFunctions.put(responseChannelType, generatorFunction);
    }

    @Override
    public Set<String> supportedDestinationTypes() {
        return generatorFunctions.keySet();
    }

    @Override
    public Set<String> supportedResponseTypes() {
        return responseChannelFunctions.keySet();
    }
}
