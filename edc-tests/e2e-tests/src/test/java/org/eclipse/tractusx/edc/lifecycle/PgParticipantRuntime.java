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

package org.eclipse.tractusx.edc.lifecycle;

import org.eclipse.edc.connector.core.vault.InMemoryVault;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.InjectionContainer;
import org.eclipse.edc.sql.testfixtures.PostgresqlLocalInstance;
import org.eclipse.tractusx.edc.token.MockDapsService;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;

public class PgParticipantRuntime extends ParticipantRuntime {

    private final String dbName;

    public PgParticipantRuntime(String moduleName, String runtimeName, String bpn, Map<String, String> properties) {
        super(moduleName, runtimeName, bpn, properties);
        this.dbName = runtimeName.toLowerCase();
        this.registerServiceMock(IdentityService.class, new MockDapsService(bpn));
        this.registerServiceMock(Vault.class, new InMemoryVaultOverride(mock(Monitor.class)));
    }

    @Override
    protected void bootExtensions(ServiceExtensionContext context, List<InjectionContainer<ServiceExtension>> serviceExtensions) {
        PostgresqlLocalInstance.createDatabase(dbName);
        super.bootExtensions(context, serviceExtensions);
    }


    private static class InMemoryVaultOverride extends InMemoryVault {

        InMemoryVaultOverride(Monitor monitor) {
            super(monitor);
        }

        @Override
        public Result<Void> deleteSecret(String s) {
            super.deleteSecret(s);
            return Result.success();
        }
    }

}
