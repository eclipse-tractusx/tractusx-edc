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

import org.eclipse.tractusx.edc.trasnferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.trasnferprocess.sftp.common.SftpProvider;
import org.eclipse.tractusx.edc.trasnferprocess.sftp.common.SftpUser;

public class NoopSftpProvider implements SftpProvider {
    @Override
    public void createUser(SftpUser user) {

    }

    @Override
    public void deleteUser(SftpUser user) {

    }

    @Override
    public void createLocation(SftpLocation location) {
    }

    @Override
    public void deleteLocation(SftpLocation location) {

    }
}
