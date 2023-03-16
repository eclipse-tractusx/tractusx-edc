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

import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Provides(SftpClientWrapper.class)
public class SftpClientExtension implements ServiceExtension {

  @Inject PipelineService pipelineService;

  @Override
  public void initialize(ServiceExtensionContext context) {
    SftpDataSinkFactory sftpDataSinkFactory = new SftpDataSinkFactory();
    SftpDataSourceFactory sftpDataSourceFactory = new SftpDataSourceFactory();
    SftpClientWrapperFactory sftpClientWrapperFactory = new SftpClientWrapperFactoryImpl();

    pipelineService.registerFactory(sftpDataSinkFactory);
    pipelineService.registerFactory(sftpDataSourceFactory);
    context.registerService(SftpClientWrapperFactory.class, sftpClientWrapperFactory);
  }
}
