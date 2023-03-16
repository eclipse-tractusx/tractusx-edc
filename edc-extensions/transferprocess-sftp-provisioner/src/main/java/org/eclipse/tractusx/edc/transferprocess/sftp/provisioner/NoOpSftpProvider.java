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

import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpProvider;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;

public class NoOpSftpProvider implements SftpProvider {

  /**
   * This provisioner does not create cloud resources. The assumption is that users and locations
   * already exist cloud-side. Thus, this method has no functionality.
   *
   * @param user The user whose credentials should be deployed.
   */
  @Override
  public void createUser(SftpUser user) {
    // do nothing
  }

  /**
   * This provisioner does not create cloud resources. The assumption is that users and locations
   * already exist cloud-side. Thus, this method has no functionality.
   *
   * @param user The user whose credentials should be deleted.
   */
  @Override
  public void deleteUser(SftpUser user) {
    // do nothing
  }

  /**
   * This provisioner does not create cloud resources. The assumption is that users and locations
   * already exist cloud-side. Thus, this method has no functionality.
   *
   * @param location The location of the cloud resource that should be made available.
   */
  @Override
  public void createLocation(SftpLocation location) {
    // do nothing
  }

  /**
   * This provisioner does not create cloud resources. The assumption is that users and locations
   * already exist cloud-side. Thus, this method has no functionality.
   *
   * @param location The location of the cloud resource that should be made unavailable.
   */
  @Override
  public void deleteLocation(SftpLocation location) {
    // do nothing
  }
}
