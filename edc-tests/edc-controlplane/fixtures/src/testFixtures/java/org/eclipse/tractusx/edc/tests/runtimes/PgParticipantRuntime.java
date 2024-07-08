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

package org.eclipse.tractusx.edc.tests.runtimes;

import org.eclipse.edc.boot.vault.InMemoryVault;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;

import java.util.Map;

class PgParticipantRuntime extends ParticipantRuntime {

    PgParticipantRuntime(String moduleName, String runtimeName, String bpn, Map<String, String> properties) {
        super(moduleName, runtimeName, bpn, properties);
        mockVault();
    }

    protected void mockVault() {
        this.registerServiceMock(Vault.class, new InMemoryVaultOverride(new ConsoleMonitor()));
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
