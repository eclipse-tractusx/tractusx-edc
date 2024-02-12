/********************************************************************************
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.edc.transferprocess.sftp.client;

import org.eclipse.edc.junit.extensions.EdcExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Does not work")
@Testcontainers
@ExtendWith(EdcExtension.class)
class SftpClientWrapperIntegrationTest extends AbstractSftpClientWrapperIntegrationTest {

    @ParameterizedTest
    @ArgumentsSource(FilesProvider.class)
    void uploadFileWithPassword(File file) throws IOException {
        var sftpUser = getPasswordUser();
        var sftpLocation = getSftpLocation(format("%s/%s/%s", SFTP_PATH_PREFIX, REMOTE_PASSWORD_UPLOAD_DIRECTORY.getFileName().toString(), file.getName()));

        var fileStream = Files.newInputStream(file.toPath());

        getSftpClient(sftpLocation, sftpUser).uploadFile(fileStream);

        var uploadedFilePath = REMOTE_PASSWORD_UPLOAD_DIRECTORY.resolve(file.getName());
        assertTrue(Files.exists(uploadedFilePath));

        var source = Files.newInputStream(file.toPath());
        var target = Files.newInputStream(uploadedFilePath);

        assertTrue(IOUtils.contentEquals(source, target),
                format("File %s should have same content as file %s", file.toPath(), uploadedFilePath));
    }

    @ParameterizedTest
    @ArgumentsSource(FilesProvider.class)
    void uploadFileWithKeyPair(File file) throws IOException {
        var sftpUser = getKeyPairUser();
        var sftpLocation =
                getSftpLocation(
                        format(
                                "%s/%s/%s",
                                SFTP_PATH_PREFIX,
                                REMOTE_KEYPAIR_UPLOAD_DIRECTORY.getFileName().toString(),
                                file.getName()));

        var fileStream = Files.newInputStream(file.toPath());

        getSftpClient(sftpLocation, sftpUser).uploadFile(fileStream);

        var uploadedFilePath = REMOTE_KEYPAIR_UPLOAD_DIRECTORY.resolve(file.getName());
        assertTrue(Files.exists(uploadedFilePath));

        var source = Files.newInputStream(file.toPath());
        var target = Files.newInputStream(uploadedFilePath);

        assertTrue(IOUtils.contentEquals(source, target),
                format("File %s should have same content as file %s", file.toPath(), uploadedFilePath));
    }

    @ParameterizedTest
    @ArgumentsSource(FilesProvider.class)
    void downloadFileWithPassword(File file) throws IOException {
        var sftpUser = getPasswordUser();
        var sftpLocation = getSftpLocation(
                format("%s/%s/%s", SFTP_PATH_PREFIX,
                        REMOTE_PASSWORD_DOWNLOAD_DIRECTORY.getFileName().toString(), file.getName()));

        var fileToUpload = Files.newInputStream(file.toPath());
        Files.copy(fileToUpload,
                REMOTE_PASSWORD_DOWNLOAD_DIRECTORY,
                StandardCopyOption.REPLACE_EXISTING);

        var source = Files.newInputStream(file.toPath());
        var target = getSftpClient(sftpLocation, sftpUser).downloadFile();

        assertTrue(IOUtils.contentEquals(source, target),
                format("File %s should have same content as file %s", file.toPath(), sftpLocation.getPath()));
    }

    @ParameterizedTest
    @ArgumentsSource(FilesProvider.class)
    void downloadFileWithKeyPair(File file) throws IOException {
        var sftpUser = getKeyPairUser();
        var sftpLocation = getSftpLocation(format("%s/%s/%s", SFTP_PATH_PREFIX, REMOTE_KEYPAIR_DOWNLOAD_DIRECTORY.getFileName().toString(), file.getName()));

        var fileToUpload = Files.newInputStream(file.toPath());
        Files.copy(fileToUpload, REMOTE_KEYPAIR_DOWNLOAD_DIRECTORY.resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);

        var source = Files.newInputStream(file.toPath());
        var target = getSftpClient(sftpLocation, sftpUser).downloadFile();

        assertTrue(IOUtils.contentEquals(source, target),
                format("File %s should have same content as file %s", file.toPath(), sftpLocation.getPath()));
    }
}
