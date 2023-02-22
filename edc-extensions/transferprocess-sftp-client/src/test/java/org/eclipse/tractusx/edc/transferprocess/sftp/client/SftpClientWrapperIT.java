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
 *       Mercedes-Benz Tech Innovation GmbH - Initial Test
 *
 */

package org.eclipse.tractusx.edc.transferprocess.sftp.client;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.Cleanup;
import lombok.SneakyThrows;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

@Testcontainers
@ExtendWith(EdcExtension.class)
class SftpClientWrapperIT extends AbstractSftpClientWrapperIT {

  @ParameterizedTest
  @SneakyThrows
  @ArgumentsSource(FilesProvider.class)
  void uploadFileWithPassword(File file) {
    final SftpUser sftpUser = getPasswordUser();
    final SftpLocation sftpLocation =
        getSftpLocation(
            String.format(
                "%s/%s/%s",
                sftpPathPrefix,
                remotePasswordUploadDirectory.getFileName().toString(),
                file.getName()));

    @Cleanup final InputStream fileStream = Files.newInputStream(file.toPath());

    getSftpClient(sftpLocation, sftpUser).uploadFile(fileStream);

    final Path uploadedFilePath = remotePasswordUploadDirectory.resolve(file.getName());
    Assertions.assertTrue(Files.exists(uploadedFilePath));

    @Cleanup final InputStream source = Files.newInputStream(file.toPath());
    @Cleanup final InputStream target = Files.newInputStream(uploadedFilePath);

    Assertions.assertTrue(
        IOUtils.contentEquals(source, target),
        String.format(
            "File %s should have same content as file %s", file.toPath(), uploadedFilePath));
  }

  @ParameterizedTest
  @SneakyThrows
  @ArgumentsSource(FilesProvider.class)
  void uploadFileWithKeyPair(File file) {
    final SftpUser sftpUser = getKeyPairUser();
    final SftpLocation sftpLocation =
        getSftpLocation(
            String.format(
                "%s/%s/%s",
                sftpPathPrefix,
                remoteKeypairUploadDirectory.getFileName().toString(),
                file.getName()));

    @Cleanup final InputStream fileStream = Files.newInputStream(file.toPath());

    getSftpClient(sftpLocation, sftpUser).uploadFile(fileStream);

    final Path uploadedFilePath = remoteKeypairUploadDirectory.resolve(file.getName());
    Assertions.assertTrue(Files.exists(uploadedFilePath));

    @Cleanup final InputStream source = Files.newInputStream(file.toPath());
    @Cleanup final InputStream target = Files.newInputStream(uploadedFilePath);

    Assertions.assertTrue(
        IOUtils.contentEquals(source, target),
        String.format(
            "File %s should have same content as file %s", file.toPath(), uploadedFilePath));
  }

  @ParameterizedTest
  @SneakyThrows
  @ArgumentsSource(FilesProvider.class)
  void downloadFileWithPassword(File file) {
    final SftpUser sftpUser = getPasswordUser();
    final SftpLocation sftpLocation =
        getSftpLocation(
            String.format(
                "%s/%s/%s",
                sftpPathPrefix,
                remotePasswordDownloadDirectory.getFileName().toString(),
                file.getName()));

    @Cleanup final InputStream fileToUpload = Files.newInputStream(file.toPath());
    Files.copy(
        fileToUpload,
        remotePasswordDownloadDirectory.resolve(file.getName()),
        StandardCopyOption.REPLACE_EXISTING);

    @Cleanup final InputStream source = Files.newInputStream(file.toPath());
    @Cleanup final InputStream target = getSftpClient(sftpLocation, sftpUser).downloadFile();

    Assertions.assertTrue(
        IOUtils.contentEquals(source, target),
        String.format(
            "File %s should have same content as file %s", file.toPath(), sftpLocation.getPath()));
  }

  @ParameterizedTest
  @SneakyThrows
  @ArgumentsSource(FilesProvider.class)
  void downloadFileWithKeyPair(File file) {
    final SftpUser sftpUser = getKeyPairUser();
    final SftpLocation sftpLocation =
        getSftpLocation(
            String.format(
                "%s/%s/%s",
                sftpPathPrefix,
                remoteKeypairDownloadDirectory.getFileName().toString(),
                file.getName()));

    @Cleanup final InputStream fileToUpload = Files.newInputStream(file.toPath());
    Files.copy(
        fileToUpload,
        remoteKeypairDownloadDirectory.resolve(file.getName()),
        StandardCopyOption.REPLACE_EXISTING);

    @Cleanup final InputStream source = Files.newInputStream(file.toPath());
    @Cleanup final InputStream target = getSftpClient(sftpLocation, sftpUser).downloadFile();

    Assertions.assertTrue(
        IOUtils.contentEquals(source, target),
        String.format(
            "File %s should have same content as file %s", file.toPath(), sftpLocation.getPath()));
  }
}
