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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.eclipse.edc.spi.types.domain.DataAddress;

@AllArgsConstructor
@Builder
@Getter
public class SftpDataAddress extends DataAddress {
  private static final String LOCATION_HOST = "locationHost";
  private static final String LOCATION_PORT = "locationPort";
  private static final String LOCATION_PATH = "locationPath";
  private static final String USER_NAME = "userName";
  private static final String USER_PASSWORD = "userPassword";
  private static final String USER_PRIVATE_KEY = "userPrivateKey";

  @Getter private static final String CONNECTION_TYPE = "sftp";
  @NonNull private final SftpUser sftpUser;
  @NonNull private final SftpLocation sftpLocation;

  public static SftpDataAddress fromDataAddress(DataAddress dataAddress) throws EdcSftpException {
    if (dataAddress instanceof SftpDataAddress) {
      return (SftpDataAddress) dataAddress;
    }

    if (!dataAddress.getType().equalsIgnoreCase("sftp")) {
      throw new EdcSftpException(
          String.format(
              "Invalid DataAddress type: %s. Expected %s.",
              dataAddress.getType(), CONNECTION_TYPE));
    }

    try {
      SftpUser sftpUser =
          SftpUser.builder()
              .name(dataAddress.getProperty(USER_NAME))
              .password(dataAddress.getProperty(USER_PASSWORD))
              .keyPair(
                  SftpUserKeyPairGenerator.getKeyPairFromPrivateKey(
                      dataAddress.getProperty(USER_PRIVATE_KEY),
                      dataAddress.getProperty(USER_NAME)))
              .build();

      SftpLocation sftpLocation =
          SftpLocation.builder()
              .host(dataAddress.getProperty(LOCATION_HOST))
              .port(Integer.valueOf(dataAddress.getProperty(LOCATION_PORT, "22")))
              .path(dataAddress.getProperty(LOCATION_PATH))
              .build();

      return SftpDataAddress.builder().sftpUser(sftpUser).sftpLocation(sftpLocation).build();
    } catch (NullPointerException e) {
      throw new EdcSftpException(e.getMessage(), e);
    } catch (NumberFormatException e) {
      throw new EdcSftpException(
          String.format(
              "Port for SftpLocation %s/%s not a number",
              dataAddress.getProperty(LOCATION_HOST), dataAddress.getProperty(LOCATION_PATH)),
          e);
    }
  }
}
