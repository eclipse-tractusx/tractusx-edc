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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.catenax.edc.trasnferprocess.sftp.common.SftpLocation;
import net.catenax.edc.trasnferprocess.sftp.common.SftpUser;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedContentResource;

@Getter
@RequiredArgsConstructor
public class SftpProvisionedContentResource extends ProvisionedContentResource {
    @NonNull
    private SftpUser sftpUser;
    @NonNull
    private SftpLocation sftpLocation;
    @NonNull
    private String transferProcessId;
}
