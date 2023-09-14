/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.dataplane.transfer.test;

import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.InjectionContainer;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.spy;

public class ParticipantRuntime extends EdcRuntimeExtension implements BeforeAllCallback, AfterAllCallback {


    public ParticipantRuntime(String moduleName, String runtimeName, Map<String, String> properties) {
        super(moduleName, runtimeName, properties);
        var monitor = spy(new ConsoleMonitor(runtimeName, ConsoleMonitor.Level.DEBUG));
        registerServiceMock(Monitor.class, monitor);
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) {
        //do nothing - we only want to start the runtime once
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        //only run this once
        super.beforeTestExecution(context);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        super.afterTestExecution(context);
    }

    public Vault getVault() {
        return getContext().getService(Vault.class);
    }

    @Override
    protected void bootExtensions(ServiceExtensionContext context, List<InjectionContainer<ServiceExtension>> serviceExtensions) {
        super.bootExtensions(context, serviceExtensions);
    }

}
