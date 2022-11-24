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

package org.eclipse.tractusx.edc.transferprocess.sftp.client;

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Provides(SftpClientWrapper.class)
public class SftpClientExtension implements ServiceExtension {
  @Override
  public void initialize(ServiceExtensionContext context) {
    SshdSftpClientWrapper sftpClient = new SshdSftpClientWrapper();
    SftpDataSinkFactory sftpDataSinkFactory = new SftpDataSinkFactory(sftpClient);
    SftpDataSourceFactory sftpDataSourceFactory = new SftpDataSourceFactory(sftpClient);

    context.registerService(SftpClientWrapper.class, sftpClient);
    context.registerService(DataSinkFactory.class, sftpDataSinkFactory);
    context.registerService(DataSourceFactory.class, sftpDataSourceFactory);
  }
}
