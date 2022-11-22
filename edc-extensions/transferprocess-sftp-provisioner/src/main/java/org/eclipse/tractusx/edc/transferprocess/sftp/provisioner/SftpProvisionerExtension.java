/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package org.eclipse.tractusx.edc.transferprocess.sftp.provisioner;

import org.eclipse.dataspaceconnector.core.policy.engine.PolicyEngineImpl;
import org.eclipse.dataspaceconnector.core.policy.engine.RuleBindingRegistryImpl;
import org.eclipse.dataspaceconnector.core.policy.engine.ScopeFilter;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.engine.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;

@Provides(NoOpSftpProvisioner.class)
public class SftpProvisionerExtension implements ServiceExtension {

  @Inject ProvisionManager provisionManager;
  @Inject Monitor monitor;

  @Override
  public String name() {
    return "Sftp Provisioner";
  }

  @Override
  public void initialize(ServiceExtensionContext context) {
    PolicyEngine policyEngine =
        new PolicyEngineImpl(new ScopeFilter(new RuleBindingRegistryImpl()));

    NoOpSftpProvider sftpProvider = new NoOpSftpProvider();
    NoOpSftpProvisioner noOpSftpProvisioner = new NoOpSftpProvisioner(policyEngine, sftpProvider);
    provisionManager.register(noOpSftpProvisioner);

    monitor.info("SftpProvisionerExtension: authentication/initialization complete.");
  }
}
