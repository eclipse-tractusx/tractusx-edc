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

package net.catnax.edc.transferprocess.sftp.client;

import lombok.Cleanup;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.catenax.edc.transferprocess.sftp.client.SftpClient;
import net.catenax.edc.transferprocess.sftp.provisioner.SftpLocation;
import net.catenax.edc.transferprocess.sftp.provisioner.SftpProvisionerExtension;
import net.catenax.edc.transferprocess.sftp.provisioner.SftpUser;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mockito;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomUtils;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static net.catenax.edc.transferprocess.sftp.provisioner.SftpProvisionerExtension.SFTP_HOST;
import static net.catenax.edc.transferprocess.sftp.provisioner.SftpProvisionerExtension.SFTP_PATH;
import static net.catenax.edc.transferprocess.sftp.provisioner.SftpProvisionerExtension.SFTP_PORT;
import static net.catenax.edc.transferprocess.sftp.provisioner.SftpProvisionerExtension.SFTP_USER_NAME;
import static net.catenax.edc.transferprocess.sftp.provisioner.SftpProvisionerExtension.SFTP_USER_PASSWORD;

@Testcontainers
@ExtendWith(EdcExtension.class)
public class SftpClientIT {
    static final String DOCKER_IMAGE_NAME = "atmoz/sftp:alpine-3.6";
    static final Map<String, String> DOCKER_ENV = Map.of("SFTP_USERS", "user:password:::upload");
    static final Path dockerVolumeDirectory;
    static final Path fileGeneratorDirectory;
    static final Path downloadDirectory;

    static {
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
            dockerVolumeDirectory = Files.createTempDirectory(SftpClientIT.class.getName(), PosixFilePermissions.asFileAttribute(fullPermission));
            fileGeneratorDirectory = Files.createTempDirectory(SftpClientIT.class.getName());
            downloadDirectory = Files.createTempDirectory(SftpClientIT.class.getName());
            Files.setPosixFilePermissions(dockerVolumeDirectory,fullPermission);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    @Container
    @ClassRule
    private static final GenericContainer<?> sftpContainer =
            new GenericContainer<>(DockerImageName.parse(DOCKER_IMAGE_NAME))
                    .withEnv(DOCKER_ENV)
                    .withExposedPorts(22)
                    .waitingFor(Wait.forListeningPort())
                    .withFileSystemBind(dockerVolumeDirectory.toAbsolutePath().toString(), "/home/user/upload");

    private ProvisionManager provisionManager;
    private TestExtension testExtension;

    @BeforeEach
    void setup(EdcExtension extension) {
        extension.setConfiguration(getSftpConfig());
        provisionManager = Mockito.mock(ProvisionManager.class);
        testExtension = new TestExtension(provisionManager);
        extension.registerSystemExtension(ServiceExtension.class, testExtension);
    }

    @AfterAll
    @SneakyThrows
    static void tearDown() {
        if (Files.exists(dockerVolumeDirectory)) {
            Files.walk(dockerVolumeDirectory).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        if (Files.exists(fileGeneratorDirectory)) {
            Files.walk(fileGeneratorDirectory).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        if (Files.exists(downloadDirectory)) {
            Files.walk(downloadDirectory).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    @ParameterizedTest
    @SneakyThrows
    @ArgumentsSource(FilesProvider.class)
    @Order(0)
    void uploadFile(File file) {
        final SftpUser sftpUser = SftpUser.builder()
                .name(getSftpConfig().get(SFTP_USER_NAME))
                .password(getSftpConfig().get(SFTP_USER_PASSWORD))
                .build();
        final SftpLocation sftpLocation = SftpLocation.builder()
                .host(getSftpConfig().get(SFTP_HOST))
                .port(Integer.parseInt(getSftpConfig().get(SFTP_PORT)))
                .path(String.format("%s/%s", getSftpConfig().get(SFTP_PATH), file.getName()))
                .build();

        @Cleanup final InputStream fileStream = Files.newInputStream(file.toPath());

        testExtension.getSftpClient().uploadFile(sftpUser, sftpLocation, fileStream);

        final Path uploadedFilePath = dockerVolumeDirectory.resolve(file.getName());
        Assertions.assertTrue(Files.exists(uploadedFilePath));
        @Cleanup final InputStream source = Files.newInputStream(file.toPath());
        @Cleanup final InputStream target = Files.newInputStream(uploadedFilePath);
        Assertions.assertTrue(IOUtils.contentEquals(source, target), String.format("File %s should have same content as file %s", file.toPath(), uploadedFilePath));
    }

    @ParameterizedTest
    @SneakyThrows
    @ArgumentsSource(FilesProvider.class)
    @Order(1)
    void downloadFile(File file) {
        final SftpUser sftpUser = SftpUser.builder()
                .name(getSftpConfig().get(SFTP_USER_NAME))
                .password(getSftpConfig().get(SFTP_USER_PASSWORD))
                .build();
        final SftpLocation sftpLocation = SftpLocation.builder()
                .host(getSftpConfig().get(SFTP_HOST))
                .port(Integer.parseInt(getSftpConfig().get(SFTP_PORT)))
                .path(String.format("%s/%s", getSftpConfig().get(SFTP_PATH), file.getName()))
                .build();

        @Cleanup final InputStream source = Files.newInputStream(file.toPath());
        @Cleanup final InputStream downloadedFileStream = testExtension.getSftpClient().downloadFile(sftpUser, sftpLocation);


        final Path downloadedFilePath = downloadDirectory.resolve(file.getName());
        Files.copy(downloadedFileStream, downloadedFilePath, StandardCopyOption.REPLACE_EXISTING);

        @Cleanup final InputStream target = Files.newInputStream(downloadedFilePath);
        Assertions.assertTrue(IOUtils.contentEquals(source, target));
    }

    private Map<String, String> getSftpConfig() {
        return Map.of(
                SFTP_HOST, "127.0.0.1",
                SFTP_PORT, sftpContainer.getFirstMappedPort().toString(),
                SFTP_PATH, "upload",
                SFTP_USER_NAME, "user",
                SFTP_USER_PASSWORD, "password");
    }

    @Getter
    @Requires(SftpClient.class)
    @RequiredArgsConstructor
    private static class TestExtension extends SftpProvisionerExtension {
        private SftpClient sftpClient;
        @NonNull
        private final ProvisionManager provisionManager;

        @Override
        public void initialize(ServiceExtensionContext context) {
            sftpClient = context.getService(SftpClient.class);
            context.registerService(ProvisionManager.class, provisionManager);
            super.initialize(context);
        }
    }

    @NoArgsConstructor
    private static class FilesProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(Arguments.of(get1KBFile()),
                    Arguments.of(get1MBFile()),
                    Arguments.of(get2MBFile())
            );
        }

        private static final int BUFFER_SIZE = 4 * 1024 * 1024;

        public File get1KBFile() {
            return generateFile(1 * 1024);
        }
        public File get1MBFile() {
            return generateFile(1 * 1024 * 1024);
        }
        public File get2MBFile() {
            return generateFile(2 * 1024 * 1024);
        }
        public File get4MBFile() {
            return generateFile(5 * 1024 * 1024);
        }
        public File get8MBFile() {
            return generateFile(8 * 1024 * 1024);
        }
        @SneakyThrows
        private File generateFile(final int byteSize) {
            Path path = fileGeneratorDirectory.resolve(String.format("%s.bin" ,byteSize));
            if (!Files.exists(path)) {
                Files.createFile(path);
                try (final OutputStream outputStream = Files.newOutputStream(path)) {
                    int bufferSize;
                    int remaining = byteSize;
                    do {
                        bufferSize = Math.min(remaining, BUFFER_SIZE);
                        byte[] chunk = RandomUtils.nextBytes(bufferSize);
                        IOUtils.write(chunk, outputStream);
                        remaining = remaining - bufferSize;
                    } while (remaining > 0);
                }
            }
            return path.toFile();
        }
    }


}
