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

import lombok.RequiredArgsConstructor;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProviderResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.EdcSftpException;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.tractusx.edc.transferprocess.sftp.provisioner.NoOpSftpProvisioner.DATA_ADDRESS_TYPE;
import static org.eclipse.tractusx.edc.transferprocess.sftp.provisioner.NoOpSftpProvisioner.PROVIDER_TYPE;

@RequiredArgsConstructor
public class SftpProviderResourceDefinitionGenerator
    implements ProviderResourceDefinitionGenerator {

  @Override
  public @Nullable ResourceDefinition generate(
      DataRequest dataRequest, DataAddress assetAddress, Policy policy) {
    if (!(assetAddress instanceof SftpDataAddress) || !assetAddress.getType().equals(DATA_ADDRESS_TYPE))
    {
      throw new EdcSftpException(String.format("Data Address %s is not an SFTP Data Address", assetAddress.getKeyName()));
    }
    SftpDataAddress sftpDataAddress = (SftpDataAddress) assetAddress;
    return new SftpProviderResourceDefinition(sftpDataAddress.getType(), PROVIDER_TYPE, sftpDataAddress.getSftpUserName(), sftpDataAddress.getSftpUserPassword(), sftpDataAddress.getSftpUserPrivateKey(), sftpDataAddress.getSftpLocationHost(),sftpDataAddress.getSftpLocationPort(), sftpDataAddress.getSftpLocationPath());
  }
}
