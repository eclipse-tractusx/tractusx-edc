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

package org.eclipse.tractusx.edc.discovery.api;

import jakarta.json.JsonObject;
import jakarta.ws.rs.container.AsyncResponse;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import org.eclipse.tractusx.edc.discovery.exceptions.UnexpectedResultApiException;
import org.eclipse.tractusx.edc.discovery.spi.ConnectorDiscoveryRequest;
import org.eclipse.tractusx.edc.discovery.spi.ConnectorDiscoveryService;
import org.eclipse.tractusx.edc.discovery.spi.ConnectorParamsDiscoveryRequest;

import java.util.concurrent.CompletionException;

/**
 * Holds the versioned-independent connector discovery logic. Version specific controllers (e.g. v3, v4alpha)
 * delegate the actual work to this controller.
 */
public class ConnectorDiscoveryController {

    private final ConnectorDiscoveryService connectorDiscoveryService;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonObjectValidatorRegistry validator;
    private final Monitor monitor;

    public ConnectorDiscoveryController(ConnectorDiscoveryService connectorDiscoveryService,
                                        TypeTransformerRegistry transformerRegistry,
                                        JsonObjectValidatorRegistry validator,
                                        Monitor monitor) {
        this.connectorDiscoveryService = connectorDiscoveryService;
        this.transformerRegistry = transformerRegistry;
        this.validator = validator;
        this.monitor = monitor;
    }

    public void discoverDspVersionParams(JsonObject inputJson, AsyncResponse response) {
        validator.validate(ConnectorParamsDiscoveryRequest.TYPE, inputJson)
                .orElseThrow(ValidationFailureException::new);

        var discoveryRequest = transformerRegistry.transform(inputJson, ConnectorParamsDiscoveryRequest.class)
                .asOptional().orElseThrow(() ->
                        new UnexpectedResultApiException("Input data could not parsed to proper request object"));

        connectorDiscoveryService.discoverVersionParams(discoveryRequest)
                .handle((result, throwable) -> {
                    handleResult(response, result, throwable);
                    return null;
                });
    }

    public void discoverConnectorServices(JsonObject inputJson, AsyncResponse response) {
        validator.validate(ConnectorDiscoveryRequest.TYPE, inputJson)
                .orElseThrow((ValidationFailureException::new));

        var request = transformerRegistry.transform(inputJson, ConnectorDiscoveryRequest.class)
                .asOptional().orElseThrow(() ->
                        new UnexpectedResultApiException("Input data could not parsed to proper request object"));

        connectorDiscoveryService.discoverConnectors(request)
                .handle((result, throwable) -> {
                    handleResult(response, result, throwable);
                    return null;
                });
    }

    private <T> void handleResult(AsyncResponse response, T result, Throwable throwable) {
        if (throwable == null) {
            response.resume(result);
        } else {
            var realCause = throwable;
            if (throwable instanceof CompletionException) {
                realCause = throwable.getCause();
            }
            monitor.warning("Exception thrown during connector discovery", realCause);
            response.resume(realCause);
        }
    }
}

