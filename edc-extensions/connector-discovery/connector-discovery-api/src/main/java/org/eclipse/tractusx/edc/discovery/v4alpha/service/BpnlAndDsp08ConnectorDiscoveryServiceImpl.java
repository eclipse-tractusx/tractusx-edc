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

package org.eclipse.tractusx.edc.discovery.v4alpha.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants;
import org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryRequest;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class BpnlAndDsp08ConnectorDiscoveryServiceImpl extends BaseConnectorDiscoveryServiceImpl {

    private static final String BPNL_PREFIX = "bpnl";

    private final BdrsClient bdrsClient;

    public BpnlAndDsp08ConnectorDiscoveryServiceImpl(
            BdrsClient bdrsClient,
            EdcHttpClient httpClient,
            DidResolverRegistry didResolver,
            ObjectMapper mapper,
            CacheConfig cacheConfig,
            Monitor monitor) {
        super(httpClient, didResolver, mapper,
                List.of(Dsp2025Constants.V_2025_1_VERSION, Dsp08Constants.V_08_VERSION), cacheConfig, monitor);
        this.bdrsClient = bdrsClient;
    }

    @Override
    public CompletableFuture<JsonArray> discoverConnectors(ConnectorDiscoveryRequest request) {
        return super.discoverConnectors(new ConnectorDiscoveryRequest(
                mapToDid(request.counterPartyId()), request.knownConnectors()));
    }

    @Override
    protected JsonObject createVersionParameterForProtocolVersion(
            String counterPartyId, String versionAddress, String versionInformation) {
        if (Dsp2025Constants.V_2025_1_VERSION.equals(versionInformation)) {
            return createVersionParameterRecord(
                    Dsp2025Constants.V_2025_1_VERSION,
                    mapToDid(counterPartyId),
                    versionAddress);
        } else if (Dsp08Constants.V_08_VERSION.equals(versionInformation) || "0.8".equals(versionInformation)) {
            // The or condition with "0.8" is for backward compatibility, because CX-0018 wrongly
            // mentioned 0.8 as version identifier
            return createVersionParameterRecord(
                    Dsp08Constants.V_08_VERSION,
                    mapToBpn(counterPartyId),
                    versionAddress);
        }
        return null;
    }

    private String mapToDid(String identifier) {
        if (identifier.toLowerCase().startsWith(DID_PREFIX)) {
            return identifier;
        } else if (identifier.toLowerCase().startsWith(BPNL_PREFIX)) {
            return Optional.ofNullable(bdrsClient.resolveDid(identifier))
                    .orElseThrow(() -> new InvalidRequestException("Bpnl %s cannot be mapped to a did".formatted(identifier)));
        }
        throw new InvalidRequestException("Given counterPartyId %s is of unknown type".formatted(identifier));
    }

    private String mapToBpn(String identifier) {
        if (identifier.toLowerCase().startsWith(BPNL_PREFIX)) {
            return identifier;
        } else if (identifier.toLowerCase().startsWith(DID_PREFIX)) {
            return Optional.ofNullable(bdrsClient.resolveBpn(identifier))
                    .orElseThrow(() -> new InvalidRequestException("Did %s cannot be mapped to a bpnl".formatted(identifier)));
        }
        throw new InvalidRequestException("Given counterPartyId %s is of unknown type".formatted(identifier));
    }
}