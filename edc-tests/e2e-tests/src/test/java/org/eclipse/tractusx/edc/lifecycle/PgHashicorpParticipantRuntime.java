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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.vault.VaultContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;

public class PgHashicorpParticipantRuntime extends PgParticipantRuntime {

    static final String DOCKER_IMAGE_NAME = "vault:1.9.6";
    static final String TOKEN = UUID.randomUUID().toString();


    public final VaultContainer<?> vaultContainer = new VaultContainer<>(DOCKER_IMAGE_NAME)
            .withVaultToken(TOKEN);
    private final String vaultDirectory;

    public PgHashicorpParticipantRuntime(String moduleName, String runtimeName, String bpn, String vaultDirectory, Map<String, String> properties) {
        super(moduleName, runtimeName, bpn, properties);
        this.vaultDirectory = vaultDirectory;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        vaultContainer.start();
        config().forEach(System::setProperty);
        super.beforeAll(context);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        super.afterAll(context);
        vaultContainer.stop();
        vaultContainer.close();
    }

    @Override
    protected void mockVault() {

    }

    private Map<String, String> config() {
        return new HashMap<>() {
            {
                put("edc.vault.hashicorp.url", format("http://%s:%s", vaultContainer.getHost(), vaultContainer.getFirstMappedPort()));
                put("edc.vault.hashicorp.token", TOKEN);
                put("edc.edr.vault.path", vaultDirectory);
            }
        };
    }
}
