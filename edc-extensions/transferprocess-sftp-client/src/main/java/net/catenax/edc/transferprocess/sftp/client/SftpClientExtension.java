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

package net.catenax.edc.transferprocess.sftp.client;

import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Provides(SftpClient.class)
public class SftpClientExtension implements ServiceExtension {
    @Override
    public void initialize(ServiceExtensionContext context) {
        SftpClientImpl sftpClient = new SftpClientImpl();
        //TODO: not here!
        sftpClient.setDisableHostVerification(true);
        context.registerService(SftpClient.class, sftpClient);
    }
}
