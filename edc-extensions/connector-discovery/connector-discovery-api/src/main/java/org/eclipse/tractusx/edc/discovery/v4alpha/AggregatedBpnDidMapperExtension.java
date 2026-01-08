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

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.tractusx.edc.discovery.v4alpha.service.AggregatedIdentifierMapper;
import org.eclipse.tractusx.edc.discovery.v4alpha.service.BpnMapper;
import org.eclipse.tractusx.edc.discovery.v4alpha.service.DidMapper;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.IdentifierToDidMapper;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

import static org.eclipse.tractusx.edc.discovery.v4alpha.ConnectorDiscoveryExtension.NAME;

@Extension(value = NAME)
public class AggregatedBpnDidMapperExtension implements ServiceExtension {
    public static final String NAME = "Identifier Mapper Extension for DID and BPNL";

    @Inject
    private BdrsClient bdrsClient;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public IdentifierToDidMapper bpnIdentityMapper() {
        return new AggregatedIdentifierMapper(new DidMapper(), new BpnMapper(bdrsClient));
    }
}
