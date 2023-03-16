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

import static org.eclipse.tractusx.edc.transferprocess.sftp.provisioner.NoOpSftpProvisioner.PROVIDER_TYPE;

import lombok.RequiredArgsConstructor;
import org.eclipse.edc.connector.transfer.spi.provision.ProviderResourceDefinitionGenerator;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.EdcSftpException;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class SftpProviderResourceDefinitionGenerator
    implements ProviderResourceDefinitionGenerator {

  @Override
  public @Nullable ResourceDefinition generate(
      DataRequest dataRequest, DataAddress assetAddress, Policy policy) {
    SftpDataAddress sftpDataAddress;
    try {
      sftpDataAddress = SftpDataAddress.fromDataAddress(assetAddress);
    } catch (EdcSftpException e) {
      return null;
    }
    return new SftpProviderResourceDefinition(PROVIDER_TYPE, sftpDataAddress);
  }

  @Override
  public boolean canGenerate(DataRequest dataRequest, DataAddress dataAddress, Policy policy) {
    try {
      SftpDataAddress.fromDataAddress(dataAddress);
    } catch (EdcSftpException e) {
      return false;
    }
    return true;
  }
}
