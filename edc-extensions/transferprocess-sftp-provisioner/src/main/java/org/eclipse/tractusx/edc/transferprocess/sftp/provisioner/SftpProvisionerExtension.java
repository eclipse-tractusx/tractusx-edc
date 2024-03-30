/********************************************************************************
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
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

package org.eclipse.tractusx.edc.transferprocess.sftp.provisioner;

import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ProviderResourceDefinitionGenerator;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Provides(NoOpSftpProvisioner.class)
public class SftpProvisionerExtension implements ServiceExtension {

    private static final String POLICY_SCOPE_CONFIG_PATH = "provisioner.sftp.policy.scope";
    private static final String DEFAULT_POLICY_SCOPE = "sftp.provisioner";
    @Inject
    ProvisionManager provisionManager;
    @Inject
    Monitor monitor;
    @Inject
    PolicyEngine policyEngine;

    @Override
    public String name() {
        return "Sftp Provisioner";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var policyScope = context.getConfig().getString(POLICY_SCOPE_CONFIG_PATH, DEFAULT_POLICY_SCOPE);

        var sftpProvider = new NoOpSftpProvider();
        var noOpSftpProvisioner = new NoOpSftpProvisioner(policyScope, policyEngine, sftpProvider);
        var generator = new SftpProviderResourceDefinitionGenerator();
        provisionManager.register(noOpSftpProvisioner);
        context.registerService(ProviderResourceDefinitionGenerator.class, generator);
        monitor.info("SftpProvisionerExtension: authentication/initialization complete.");
    }
}
