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

package net.catenax.edc.transferprocess.sftp.provisioner;

import org.eclipse.dataspaceconnector.core.base.policy.PolicyEngineImpl;
import org.eclipse.dataspaceconnector.core.base.policy.RuleBindingRegistryImpl;
import org.eclipse.dataspaceconnector.core.base.policy.ScopeFilter;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;

import java.nio.charset.StandardCharsets;

@Provides(SftpProvisioner.class)
public class SftpProvisionerExtension implements ServiceExtension {

    @EdcSetting(required = true)
    public static final String SFTP_HOST = "edc.transfer.sftp.location.host";

    @EdcSetting(required = true)
    public static final String SFTP_PORT = "edc.transfer.sftp.location.port";

    @EdcSetting(required = true)
    public static final String SFTP_PATH = "edc.transfer.sftp.location.path";

    @EdcSetting(required = true)
    public static final String SFTP_USER_NAME = "edc.transfer.sftp.user.name";

    @EdcSetting(required = true)
    public static final String SFTP_USER_KEY = "edc.transfer.sftp.user.key";

    @Inject
    ProvisionManager provisionManager;

    @Override
    public String name() {
        return "Sftp Provisioner";
    }

    private SftpProvisioner sftpProvisioner;

    @Override
    public void initialize(ServiceExtensionContext context) {
        PolicyEngine policyEngine = new PolicyEngineImpl(new ScopeFilter(new RuleBindingRegistryImpl()));

        final String sftpHost = context.getSetting(SFTP_HOST, "localhost");
        final Integer sftpPort = context.getSetting(SFTP_PORT, 22);
        final String sftpPath = context.getSetting(SFTP_PATH, "test");
        SftpLocationFactory locationFactory = new ConfigBackedSftpLocationFactory(sftpHost, sftpPort, sftpPath);

        final String sftpName = context.getSetting(SFTP_USER_NAME, "user");
        final byte[] sftpKey = context.getSetting(SFTP_USER_KEY, "key").getBytes(StandardCharsets.UTF_8);
        SftpUserFactory userFactory = new ConfigBackedSftpUserFactory(sftpName, sftpKey);

        SftpProvider sftpProvider = new NoopSftpProvider();
        sftpProvisioner = new SftpProvisioner(policyEngine, locationFactory, userFactory, sftpProvider);
        provisionManager.register(sftpProvisioner);
        context.getMonitor().info("SftpProvisionerExtension: authentication/initialization complete.");
    }
}
