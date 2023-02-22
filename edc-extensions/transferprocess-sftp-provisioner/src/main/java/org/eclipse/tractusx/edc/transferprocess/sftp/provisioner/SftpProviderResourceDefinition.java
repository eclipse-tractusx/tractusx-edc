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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;

@Getter
@RequiredArgsConstructor
public class SftpProviderResourceDefinition extends ResourceDefinition {
  @NonNull private String providerType;
  @NonNull private SftpDataAddress sftpDataAddress;

  @Override
  public <R extends ResourceDefinition, B extends Builder<R, B>> B toBuilder() {
    return null;
  }
}
