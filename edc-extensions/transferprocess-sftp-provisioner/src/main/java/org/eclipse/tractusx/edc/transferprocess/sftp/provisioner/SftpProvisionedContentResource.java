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

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedContentResource;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;

@Getter
@RequiredArgsConstructor
@Builder
public class SftpProvisionedContentResource extends ProvisionedContentResource {
  @NonNull private String providerType;
  @NonNull private Policy scopedPolicy;
  @NonNull private SftpDataAddress sftpDataAddress;
  @NonNull private String provisionedResourceId;
}
