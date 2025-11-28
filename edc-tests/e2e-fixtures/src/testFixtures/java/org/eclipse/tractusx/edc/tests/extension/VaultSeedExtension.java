/*
 * Copyright (c) 2024 Cofinity-X
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

package org.eclipse.tractusx.edc.tests.extension;

import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.Map;

public class VaultSeedExtension implements ServiceExtension {

    private final Map<String, String> secrets;
    @Inject
    private Vault vault;
    @Inject
    private SingleParticipantContextSupplier singleParticipantContextSupplier;

    public VaultSeedExtension(Map<String, String> secrets) {
        this.secrets = secrets;
    }

    @Override
    public String name() {
        return "Vault Seed";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var participantContext = singleParticipantContextSupplier.get().orElseThrow(f -> new EdcException(f.getFailureDetail()));
        secrets.forEach((key, value) -> vault.storeSecret(participantContext.getParticipantContextId(), key, value));
    }
}
