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

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Provides(SftpClientWrapper.class)
public class SftpClientExtension implements ServiceExtension {

  @Inject PipelineService pipelineService;

  @Override
  public void initialize(ServiceExtensionContext context) {
    SftpClientWrapperImpl sftpClientWrapper = new SftpClientWrapperImpl();
    SftpDataSinkFactory sftpDataSinkFactory = new SftpDataSinkFactory(sftpClientWrapper);
    SftpDataSourceFactory sftpDataSourceFactory = new SftpDataSourceFactory(sftpClientWrapper);

    context.registerService(SftpClientWrapper.class, sftpClientWrapper);
    pipelineService.registerFactory(sftpDataSinkFactory);
    pipelineService.registerFactory(sftpDataSourceFactory);
  }
}
