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
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.CacheConfig;

import java.util.List;

public class DefaultConnectorDiscoveryServiceImpl extends BaseConnectorDiscoveryServiceImpl {

    public DefaultConnectorDiscoveryServiceImpl(
            EdcHttpClient httpClient,
            DidResolverRegistry didResolver,
            ObjectMapper mapper,
            CacheConfig cacheConfig,
            Monitor monitor) {
        super(httpClient, didResolver, mapper, List.of(Dsp2025Constants.V_2025_1_VERSION), cacheConfig, monitor);
    }

    @Override
    protected VersionParameters createVersionParameterForProtocolVersion(
            String counterPartyId, String versionAddress, String version) {
        if (!counterPartyId.toLowerCase().startsWith(DID_PREFIX)) {
            throw new InvalidRequestException(
                    "CounterPartyId used not supported, must be a did: %s".formatted(counterPartyId));
        }
        if (Dsp2025Constants.V_2025_1_VERSION.equals(version)) {
            return new VersionParameters(counterPartyId, versionAddress, version);
        }
        return null;
    }
}
