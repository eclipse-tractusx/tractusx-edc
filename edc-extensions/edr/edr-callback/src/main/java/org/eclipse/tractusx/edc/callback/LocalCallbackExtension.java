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

import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackProtocolResolverRegistry;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.spi.callback.InProcessCallbackRegistry;

import static org.eclipse.tractusx.edc.callback.InProcessCallbackMessageDispatcher.CALLBACK_EVENT_LOCAL;

@Extension(LocalCallbackExtension.NAME)
public class LocalCallbackExtension implements ServiceExtension {
    public static final String NAME = "Local callbacks extension";

    public static final String LOCAL = "local";

    @Inject
    private RemoteMessageDispatcherRegistry registry;
    @Inject
    private CallbackProtocolResolverRegistry resolverRegistry;
    @Inject
    private TransferProcessService transferProcessService;
    @Inject
    private InProcessCallbackRegistry callbackRegistry;
    @Inject
    private Monitor monitor;
    @Inject
    private SingleParticipantContextSupplier singleParticipantContextSupplier;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        callbackRegistry.registerHandler(new ContractNegotiationCallback(transferProcessService, monitor, singleParticipantContextSupplier));

        resolverRegistry.registerResolver(this::resolveProtocol);
        registry.register(CALLBACK_EVENT_LOCAL, new InProcessCallbackMessageDispatcher(callbackRegistry));

    }

    private String resolveProtocol(String scheme) {

        if (scheme.equalsIgnoreCase(LOCAL)) {
            return CALLBACK_EVENT_LOCAL;
        }
        return null;
    }
}
