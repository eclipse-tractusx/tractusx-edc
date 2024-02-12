/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

abstract class AbstractSftpClientWrapperIntegrationTest {
    static final String DOCKER_IMAGE_NAME = "atmoz/sftp:alpine-3.6";
    static final String SFTP_PATH_PREFIX = "transfer";
    static final Map<String, String> DOCKER_ENV =
            Map.of("SFTP_USERS", String.format("user:password:::%s", SFTP_PATH_PREFIX));
    static final Path DOCKER_VOLUME_DIRECTORY;
    static final Path REMOTE_PASSWORD_UPLOAD_DIRECTORY;
    static final Path REMOTE_PASSWORD_DOWNLOAD_DIRECTORY;
    static final Path REMOTE_KEYPAIR_UPLOAD_DIRECTORY;
    static final Path REMOTE_KEYPAIR_DOWNLOAD_DIRECTORY;
    static final Path LOCAL_UPLOAD_AND_GENERATOR_DIRECTORY;
    static final Path KEY_DIRECTORY;
    static final Path PUBLIC_KEY_PATH;
    static final KeyPair KEY_PAIR;
    @Container
    @ClassRule
    private static final GenericContainer<?> SFTP_CONTAINER;

    static {
        KEY_PAIR = generateKeyPair();

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

            DOCKER_VOLUME_DIRECTORY = Files.createTempDirectory(SftpClientWrapperIntegrationTest.class.getName());
            LOCAL_UPLOAD_AND_GENERATOR_DIRECTORY =
                    Files.createTempDirectory(SftpClientWrapperIntegrationTest.class.getName());
            REMOTE_PASSWORD_UPLOAD_DIRECTORY =
                    Files.createDirectory(DOCKER_VOLUME_DIRECTORY.resolve("passwordUpload"));
            REMOTE_PASSWORD_DOWNLOAD_DIRECTORY =
                    Files.createDirectory(DOCKER_VOLUME_DIRECTORY.resolve("passwordDownload"));
            REMOTE_KEYPAIR_UPLOAD_DIRECTORY =
                    Files.createDirectory(DOCKER_VOLUME_DIRECTORY.resolve("keypairUpload"));
            REMOTE_KEYPAIR_DOWNLOAD_DIRECTORY =
                    Files.createDirectory(DOCKER_VOLUME_DIRECTORY.resolve("keypairDownload"));
            KEY_DIRECTORY = Files.createTempDirectory(SftpClientWrapperIntegrationTest.class.getName());
            PUBLIC_KEY_PATH = KEY_DIRECTORY.resolve("public");

            try (var fileWriter =
                         new OutputStreamWriter(new FileOutputStream(PUBLIC_KEY_PATH.toString()))) {
                final RSAPublicKey publicKey = (RSAPublicKey) KEY_PAIR.getPublic();
                final RSAKeyParameters publicKeyParameters =
                        new RSAKeyParameters(false, publicKey.getModulus(), publicKey.getPublicExponent());
                byte[] encodedKey = OpenSSHPublicKeyUtil.encodePublicKey(publicKeyParameters);
                String keyString = Base64.getEncoder().encodeToString(encodedKey);
                String authKeysEntry = String.format("ssh-rsa %s", keyString);
                fileWriter.write(authKeysEntry);
            }

            Files.setPosixFilePermissions(DOCKER_VOLUME_DIRECTORY, fullPermission);
            Files.setPosixFilePermissions(REMOTE_PASSWORD_UPLOAD_DIRECTORY, fullPermission);
            Files.setPosixFilePermissions(REMOTE_PASSWORD_DOWNLOAD_DIRECTORY, fullPermission);
            Files.setPosixFilePermissions(REMOTE_KEYPAIR_UPLOAD_DIRECTORY, fullPermission);
            Files.setPosixFilePermissions(REMOTE_KEYPAIR_DOWNLOAD_DIRECTORY, fullPermission);
            Files.setPosixFilePermissions(KEY_DIRECTORY, fullPermission);
        } catch (IOException e) {
            throw new RuntimeException();
        }

        SFTP_CONTAINER =
                new GenericContainer<>(DockerImageName.parse(DOCKER_IMAGE_NAME))
                        .withEnv(DOCKER_ENV)
                        .withExposedPorts(22)
                        .waitingFor(Wait.forListeningPort())
                        .withFileSystemBind(
                                DOCKER_VOLUME_DIRECTORY.toAbsolutePath().toString(),
                                String.format("/home/user/%s", SFTP_PATH_PREFIX))
                        .withFileSystemBind(KEY_DIRECTORY.toAbsolutePath().toString(), "/home/user/keys");
        SFTP_CONTAINER.start();

        await().atMost(10, SECONDS).until(SFTP_CONTAINER::isRunning);

        try {
            SFTP_CONTAINER.execInContainer("mkdir", "-p", "/home/user/.ssh");
            SFTP_CONTAINER.execInContainer("chmod", "700", "/home/user/.ssh");
            SFTP_CONTAINER.execInContainer("chown", "user", "/home/user/.ssh/");
            SFTP_CONTAINER.execInContainer(
                    "cp", "-f", "/home/user/keys/public", "/home/user/.ssh/authorized_keys");
            SFTP_CONTAINER.execInContainer("chown", "user", "/home/user/.ssh/authorized_keys");
            SFTP_CONTAINER.execInContainer("chmod", "600", "/home/user/.ssh/authorized_keys");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException();
        }
    }

    @AfterAll
    static void tearDown() throws IOException {
        if (Files.exists(DOCKER_VOLUME_DIRECTORY)) {
            Files.walk(DOCKER_VOLUME_DIRECTORY)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        if (Files.exists(LOCAL_UPLOAD_AND_GENERATOR_DIRECTORY)) {
            Files.walk(LOCAL_UPLOAD_AND_GENERATOR_DIRECTORY)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        if (Files.exists(KEY_DIRECTORY)) {
            Files.walk(KEY_DIRECTORY)
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
        return SftpUser.Builder.newInstance().name("user").keyPair(KEY_PAIR).build();
    }

    protected SftpLocation getSftpLocation(String path) {
        return SftpLocation.Builder.newInstance()
                .host("127.0.0.1")
                .port(SFTP_CONTAINER.getFirstMappedPort())
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
                    Arguments.of(get1KbFile()), Arguments.of(get1MbFile()), Arguments.of(get2MbFile()));
        }

        public File get1KbFile() {
            return generateFile(1024);
        }

        public File get1MbFile() {
            return generateFile(1024 * 1024);
        }

        public File get2MbFile() {
            return generateFile(2 * 1024 * 1024);
        }

        private File generateFile(final int byteSize) {
            Path path = LOCAL_UPLOAD_AND_GENERATOR_DIRECTORY.resolve(String.format("%s.bin", byteSize));
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
