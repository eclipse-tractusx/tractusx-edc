/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */


package org.eclipse.tractusx.edc.transferprocess.sftp.client;

import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomUtils;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

abstract class AbstractSftpClientWrapperIT {
    static final String DOCKER_IMAGE_NAME = "atmoz/sftp:alpine-3.6";
    static final String sftpPathPrefix = "transfer";
    static final Map<String, String> DOCKER_ENV =
            Map.of("SFTP_USERS", String.format("user:password:::%s", sftpPathPrefix));
    static final Path dockerVolumeDirectory;
    static final Path remotePasswordUploadDirectory;
    static final Path remotePasswordDownloadDirectory;
    static final Path remoteKeypairUploadDirectory;
    static final Path remoteKeypairDownloadDirectory;
    static final Path localUploadAndGeneratorDirectory;
    static final Path keyDirectory;
    static final Path publicKeyPath;
    static final KeyPair keyPair;
    @Container
    @ClassRule
    private static final GenericContainer<?> sftpContainer;

    static {
        keyPair = generateKeyPair();

        try {
            Set<PosixFilePermission> fullPermission = new HashSet<PosixFilePermission>();
            fullPermission.add(PosixFilePermission.OWNER_EXECUTE);
            fullPermission.add(PosixFilePermission.OWNER_READ);
            fullPermission.add(PosixFilePermission.OWNER_WRITE);
            fullPermission.add(PosixFilePermission.GROUP_EXECUTE);
            fullPermission.add(PosixFilePermission.GROUP_READ);
            fullPermission.add(PosixFilePermission.GROUP_WRITE);
            fullPermission.add(PosixFilePermission.OTHERS_EXECUTE);
            fullPermission.add(PosixFilePermission.OTHERS_READ);
            fullPermission.add(PosixFilePermission.OTHERS_WRITE);

            dockerVolumeDirectory = Files.createTempDirectory(SftpClientWrapperIT.class.getName());
            localUploadAndGeneratorDirectory =
                    Files.createTempDirectory(SftpClientWrapperIT.class.getName());
            remotePasswordUploadDirectory =
                    Files.createDirectory(dockerVolumeDirectory.resolve("passwordUpload"));
            remotePasswordDownloadDirectory =
                    Files.createDirectory(dockerVolumeDirectory.resolve("passwordDownload"));
            remoteKeypairUploadDirectory =
                    Files.createDirectory(dockerVolumeDirectory.resolve("keypairUpload"));
            remoteKeypairDownloadDirectory =
                    Files.createDirectory(dockerVolumeDirectory.resolve("keypairDownload"));
            keyDirectory = Files.createTempDirectory(SftpClientWrapperIT.class.getName());
            publicKeyPath = keyDirectory.resolve("public");

            try (final OutputStreamWriter fileWriter =
                         new OutputStreamWriter(new FileOutputStream(publicKeyPath.toString()))) {
                final RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
                final RSAKeyParameters publicKeyParameters =
                        new RSAKeyParameters(false, publicKey.getModulus(), publicKey.getPublicExponent());
                byte[] encodedKey = OpenSSHPublicKeyUtil.encodePublicKey(publicKeyParameters);
                String keyString = Base64.getEncoder().encodeToString(encodedKey);
                String authKeysEntry = String.format("ssh-rsa %s", keyString);
                fileWriter.write(authKeysEntry);
            }

            Files.setPosixFilePermissions(dockerVolumeDirectory, fullPermission);
            Files.setPosixFilePermissions(remotePasswordUploadDirectory, fullPermission);
            Files.setPosixFilePermissions(remotePasswordDownloadDirectory, fullPermission);
            Files.setPosixFilePermissions(remoteKeypairUploadDirectory, fullPermission);
            Files.setPosixFilePermissions(remoteKeypairDownloadDirectory, fullPermission);
            Files.setPosixFilePermissions(keyDirectory, fullPermission);
        } catch (IOException e) {
            throw new RuntimeException();
        }

        sftpContainer = new GenericContainer<>(DockerImageName.parse(DOCKER_IMAGE_NAME))
                .withEnv(DOCKER_ENV)
                .withExposedPorts(22)
                .waitingFor(Wait.forListeningPort())
                .withFileSystemBind(
                        dockerVolumeDirectory.toAbsolutePath().toString(),
                        String.format("/home/user/%s", sftpPathPrefix))
                .withFileSystemBind(keyDirectory.toAbsolutePath().toString(), "/home/user/keys");
        sftpContainer.start();

        await().atMost(10, SECONDS).until(sftpContainer::isRunning);

        try {
            sftpContainer.execInContainer("mkdir", "-p", "/home/user/.ssh");
            sftpContainer.execInContainer("chmod", "700", "/home/user/.ssh");
            sftpContainer.execInContainer("chown", "user", "/home/user/.ssh/");
            sftpContainer.execInContainer(
                    "cp", "-f", "/home/user/keys/public", "/home/user/.ssh/authorized_keys");
            sftpContainer.execInContainer("chown", "user", "/home/user/.ssh/authorized_keys");
            sftpContainer.execInContainer("chmod", "600", "/home/user/.ssh/authorized_keys");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException();
        }
    }

    @AfterAll
    static void tearDown() throws IOException {
        if (Files.exists(dockerVolumeDirectory)) {
            Files.walk(dockerVolumeDirectory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        if (Files.exists(localUploadAndGeneratorDirectory)) {
            Files.walk(localUploadAndGeneratorDirectory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        if (Files.exists(keyDirectory)) {
            Files.walk(keyDirectory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    protected SftpUser getPasswordUser() {
        return SftpUser.Builder.newInstance().name("user").password("password").build();
    }

    protected SftpUser getKeyPairUser() {
        return SftpUser.Builder.newInstance().name("user").keyPair(keyPair).build();
    }

    protected SftpLocation getSftpLocation(String path) {
        return SftpLocation.Builder.newInstance()
                .host("127.0.0.1")
                .port(sftpContainer.getFirstMappedPort())
                .path(path)
                .build();
    }

    protected SftpClientWrapper getSftpClient(SftpLocation location, SftpUser sftpUser) {
        SftpClientConfig config =
                SftpClientConfig.Builder.newInstance()
                        .sftpLocation(location)
                        .sftpUser(sftpUser)
                        .hostVerification(false)
                        .build();
        return new SftpClientWrapperImpl(config);
    }

    protected static class FilesProvider implements ArgumentsProvider {
        private static final int BUFFER_SIZE = 4 * 1024 * 1024;

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(get1KBFile()), Arguments.of(get1MBFile()), Arguments.of(get2MBFile()));
        }

        public File get1KBFile() {
            return generateFile(1024);
        }

        public File get1MBFile() {
            return generateFile(1024 * 1024);
        }

        public File get2MBFile() {
            return generateFile(2 * 1024 * 1024);
        }

        private File generateFile(final int byteSize) {
            Path path = localUploadAndGeneratorDirectory.resolve(String.format("%s.bin", byteSize));
            if (!Files.exists(path)) {
                try {
                    Files.createFile(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try (var outputStream = Files.newOutputStream(path)) {
                    int bufferSize;
                    int remaining = byteSize;
                    do {
                        bufferSize = Math.min(remaining, BUFFER_SIZE);
                        byte[] chunk = RandomUtils.nextBytes(bufferSize);
                        IOUtils.write(chunk, outputStream);
                        remaining = remaining - bufferSize;
                    } while (remaining > 0);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return path.toFile();
        }
    }
}
