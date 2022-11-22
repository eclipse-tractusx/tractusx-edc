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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class SftpProviderResourceDefinition extends ResourceDefinition {
  @NonNull private String dataAddressType;
  @NonNull private String providerType;
  @NonNull private String sftpUserName;
  @ToString.Exclude private String sftpUserPassword;
  @ToString.Exclude private byte[] sftpUserPrivateKey;
  @NonNull private String sftpLocationUrl;
  @NonNull private Integer sftpLocationPort;
  @NonNull private String sftpLocationPath;

  @Override
  public <RD extends ResourceDefinition, B extends Builder<RD, B>> B toBuilder() {
    return null;
  }
}
