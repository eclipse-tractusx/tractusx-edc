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

import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;

public class SftpProviderResourceDefinition extends ResourceDefinition {
    private final String providerType;
    private final SftpDataAddress sftpDataAddress;

    public SftpProviderResourceDefinition(String providerType, SftpDataAddress sftpDataAddress) {
        this.providerType = providerType;
        this.sftpDataAddress = sftpDataAddress;
    }


    public String getProviderType() {
        return providerType;
    }

    public SftpDataAddress getSftpDataAddress() {
        return sftpDataAddress;
    }

    @Override
    public <R extends ResourceDefinition, B extends Builder<R, B>> B toBuilder() {
        return null;
    }
}
