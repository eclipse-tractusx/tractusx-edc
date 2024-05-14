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

package org.eclipse.tractusx.edc.lifecycle;

import org.eclipse.edc.runtime.metamodel.annotation.BaseExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.stream.Stream;

import static org.eclipse.tractusx.edc.core.utils.ConfigUtil.propertyCompatibility;

@BaseExtension
public class VaultSeedExtension implements ServiceExtension {
    @Setting
    private static final String TX_VAULT_SEED = "tx.edc.vault.seed.secrets";
    @Deprecated(since = "0.7.1")
    private static final String TX_VAULT_SEED_DEPRECATED = "tx.vault.seed.secrets";

    @Inject
    private Vault vault;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var seedSecrets = propertyCompatibility(context, TX_VAULT_SEED, TX_VAULT_SEED_DEPRECATED, null);
        if (seedSecrets != null) {
            Stream.of(seedSecrets.split(";"))
                    .filter(pair -> pair.contains(":"))
                    .map(kvp -> kvp.split(":", 2))
                    .filter(kvp -> kvp.length >= 2)
                    .forEach(pair -> vault.storeSecret(pair[0], pair[1]));
        }
    }
}
