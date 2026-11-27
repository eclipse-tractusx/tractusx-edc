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

package org.eclipse.tractusx.edc.callback;

import org.eclipse.edc.connector.controlplane.callback.dispatcher.CallbackHttpClient;
import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackClient;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.spi.callback.InProcessCallbackRegistry;

/**
 * Handles callback addresses with the {@code local} scheme in-process, e.g. the automatic EDR negotiation of the
 * EDR API ({@code local://adapter}).
 * <p>
 * Since EDC 0.18 the upstream callback dispatcher sends every callback through a single {@link CallbackClient}, so this
 * extension provides one that handles {@code local} callbacks and delegates all others to the upstream
 * {@link CallbackHttpClient}.
 */
@Extension(LocalCallbackExtension.NAME)
public class LocalCallbackExtension implements ServiceExtension {

    public static final String NAME = "Local callbacks extension";

    @Inject
    private TransferProcessService transferProcessService;
    @Inject
    private InProcessCallbackRegistry callbackRegistry;
    @Inject
    private Monitor monitor;
    @Inject
    private SingleParticipantContextSupplier singleParticipantContextSupplier;
    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private TypeManager typeManager;
    @Inject
    private Vault vault;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        callbackRegistry.registerHandler(new ContractNegotiationCallback(transferProcessService, monitor, singleParticipantContextSupplier));
    }

    @Provider
    public CallbackClient callbackClient() {
        var httpCallbackClient = new CallbackHttpClient(httpClient, typeManager.getMapper(), vault);
        return new LocalCallbackClient(callbackRegistry, httpCallbackClient, monitor.withPrefix(LocalCallbackClient.class.getSimpleName()));
    }
}
