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
package org.eclipse.tractusx.edc.vault.memory;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.*;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.stream.Stream;

@Provides({PrivateKeyResolver.class, CertificateResolver.class})
@Extension(value = "In-memory vault extension", categories = {"vault", "security"})
public class VaultMemoryExtension implements ServiceExtension {

    @Setting(value = "Secrets with which the vault gets initially populated. Specify as comma-separated list of key:secret pairs.")
    public static final String VAULT_MEMORY_SECRETS_PROPERTY = "edc.vault.secrets";
    public static final String NAME = "In-Memory Vault Extension";

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public Vault createInMemVault(ServiceExtensionContext context) {
        var seedSecrets = context.getSetting(VAULT_MEMORY_SECRETS_PROPERTY, null);
        var vault = new InMemoryVault(context.getMonitor());
        context.registerService(PrivateKeyResolver.class, new VaultPrivateKeyResolver(vault));
        context.registerService(CertificateResolver.class, new VaultCertificateResolver(vault));
        if (seedSecrets != null) {
            Stream.of(seedSecrets.split(";"))
                    .filter(pair -> pair.contains(":"))
                    .map(kvp -> kvp.split(":", 2))
                    .filter(kvp -> kvp.length >= 2)
                    .forEach(pair -> vault.storeSecret(pair[0], pair[1]));
        }
        return vault;
    }
}
