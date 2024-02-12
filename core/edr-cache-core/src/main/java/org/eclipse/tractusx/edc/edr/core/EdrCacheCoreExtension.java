/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.edr.core;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.edr.core.defaults.EdrCacheEntryPropertyLookup;
import org.eclipse.tractusx.edc.edr.core.defaults.InMemoryEndpointDataReferenceCache;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;

/**
 * Registers default services for the EDR cache.
 */
@Extension(value = EdrCacheCoreExtension.NAME)
public class EdrCacheCoreExtension implements ServiceExtension {

    static final String NAME = "EDR Cache Core";

    @Inject
    private CriterionOperatorRegistry operatorRegistry;

    @Override
    public String name() {
        return NAME;
    }


    @Override
    public void initialize(ServiceExtensionContext context) {
        operatorRegistry.registerPropertyLookup(new EdrCacheEntryPropertyLookup());
    }

    @Provider(isDefault = true)
    public EndpointDataReferenceCache edrCache() {
        return new InMemoryEndpointDataReferenceCache(operatorRegistry);
    }

}
