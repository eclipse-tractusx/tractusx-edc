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

import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.edr.core.service.EdrServiceImpl;
import org.eclipse.tractusx.edc.edr.spi.index.lock.EndpointDataReferenceLock;
import org.eclipse.tractusx.edc.edr.spi.service.EdrService;
import org.eclipse.tractusx.edc.spi.tokenrefresh.common.TokenRefreshHandler;

/**
 * Registers default services for the EDR cache.
 */
@Extension(value = EdrCoreServiceExtension.NAME)
public class EdrCoreServiceExtension implements ServiceExtension {
    protected static final String NAME = "EDR Core Service extension";

    @Inject
    private Monitor monitor;

    @Inject
    private EndpointDataReferenceStore edrStore;

    @Inject
    private TokenRefreshHandler tokenRefreshHandler;

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private EndpointDataReferenceLock edrLock;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public EdrService edrService() {
        return new EdrServiceImpl(edrStore, tokenRefreshHandler, transactionContext, monitor, edrLock);
    }
}
