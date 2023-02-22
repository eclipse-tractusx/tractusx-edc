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

package org.eclipse.tractusx.edc.transferprocess.sftp.common;

import org.eclipse.edc.spi.EdcException;

public class EdcSftpException extends EdcException {
  public EdcSftpException(String message) {
    super(message);
  }

  public EdcSftpException(String message, Throwable cause) {
    super(message, cause);
  }

  public EdcSftpException(Throwable cause) {
    super(cause);
  }

  public EdcSftpException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
