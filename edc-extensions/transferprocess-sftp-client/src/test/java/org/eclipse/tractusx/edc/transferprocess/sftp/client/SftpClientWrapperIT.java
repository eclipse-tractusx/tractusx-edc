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

import lombok.Cleanup;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.tractusx.edc.trasnferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.trasnferprocess.sftp.common.SftpUser;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomUtils;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.eclipse.tractusx.edc.trasnferprocess.sftp.common.SftpSettings.SFTP_HOST;
import static org.eclipse.tractusx.edc.trasnferprocess.sftp.common.SftpSettings.SFTP_PATH;
import static org.eclipse.tractusx.edc.trasnferprocess.sftp.common.SftpSettings.SFTP_PORT;
import static org.eclipse.tractusx.edc.trasnferprocess.sftp.common.SftpSettings.SFTP_USER_NAME;
import static org.eclipse.tractusx.edc.trasnferprocess.sftp.common.SftpSettings.SFTP_USER_PASSWORD;

@Testcontainers
@ExtendWith(EdcExtension.class)
public class SftpClientWrapperIT {
  static final String DOCKER_IMAGE_NAME = "atmoz/sftp:alpine-3.6";
  static final Map<String, String> DOCKER_ENV = Map.of("SFTP_USERS", "user:password:::transfer");
  static final Path dockerVolumeDirectory;
  static final Path remoteUploadDirectory;
  static final Path remoteDownloadDirectory;
  static final Path localUploadAndGeneratorDirectory;
  static final Path localDownloadDirectory;
  static final Path keyDirectory;
  static final Path publicKeyPath;
  static final KeyPair keyPair;

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
      localDownloadDirectory = Files.createTempDirectory(SftpClientWrapperIT.class.getName());
      remoteUploadDirectory = Files.createDirectory(dockerVolumeDirectory.resolve("upload"));
      remoteDownloadDirectory = Files.createDirectory(dockerVolumeDirectory.resolve("download"));
      keyDirectory = Files.createTempDirectory(SftpClientWrapperIT.class.getName());
      publicKeyPath = keyDirectory.resolve("public");

      try (final OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(publicKeyPath.toString()))) {
        final RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        final RSAKeyParameters publicKeyParameters = new RSAKeyParameters(false, publicKey.getModulus(), publicKey.getPublicExponent());
        byte[] encodedKey = OpenSSHPublicKeyUtil.encodePublicKey(publicKeyParameters);
        String keyString = Base64.getEncoder().encodeToString(encodedKey);
        String authKeysEntry = String.format("ssh-rsa %s", keyString);
        fileWriter.write(authKeysEntry);
      }

      Files.setPosixFilePermissions(dockerVolumeDirectory, fullPermission);
      Files.setPosixFilePermissions(remoteUploadDirectory, fullPermission);
      Files.setPosixFilePermissions(remoteDownloadDirectory, fullPermission);
      Files.setPosixFilePermissions(keyDirectory, fullPermission);

    } catch (IOException e) {
      throw new RuntimeException();
    }
  }

  @Container @ClassRule
  private static final GenericContainer<?> sftpContainer =
      new GenericContainer<>(DockerImageName.parse(DOCKER_IMAGE_NAME))
          .withEnv(DOCKER_ENV)
          .withExposedPorts(22)
          .waitingFor(Wait.forListeningPort())
          .withFileSystemBind(
              dockerVolumeDirectory.toAbsolutePath().toString(), "/home/user/transfer")
          .withFileSystemBind(keyDirectory.toAbsolutePath().toString(), "/home/user/keys");

  private final SshdSftpClient sftpClient = new SshdSftpClient();

  @BeforeEach
  @SneakyThrows
  void setup() {
    sftpClient.setDisableHostVerification(true);

    sftpContainer.execInContainer("mkdir", "-p", "/home/user/.ssh");
    sftpContainer.execInContainer("chmod", "700", "/home/user/.ssh");
    sftpContainer.execInContainer("chown", "user", "/home/user/.ssh/");
    sftpContainer.execInContainer("cp", "-f", "/home/user/keys/public", "/home/user/.ssh/authorized_keys");
    sftpContainer.execInContainer("chown", "user", "/home/user/.ssh/authorized_keys");
    sftpContainer.execInContainer("chmod", "600", "/home/user/.ssh/authorized_keys");
  }

  @AfterAll
  @SneakyThrows
  static void tearDown() {
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
    if (Files.exists(localDownloadDirectory)) {
      Files.walk(localDownloadDirectory)
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

  @ParameterizedTest
  @SneakyThrows
  @ArgumentsSource(FilesProvider.class)
  void uploadFileWithPassword(File file) {
    String remoteFileName = String.format("pwUpload_%s", file.getName());
    final SftpUser sftpUser =
        SftpUser.builder()
            .name(getSftpPasswordConfig().get(SFTP_USER_NAME))
            .password(getSftpPasswordConfig().get(SFTP_USER_PASSWORD))
            .build();
    final SftpLocation sftpLocation =
        SftpLocation.builder()
            .host(getSftpPasswordConfig().get(SFTP_HOST))
            .port(Integer.parseInt(getSftpPasswordConfig().get(SFTP_PORT)))
            .path(
                String.format(
                    "%s/%s/%s", getSftpPasswordConfig().get(SFTP_PATH), "upload", remoteFileName))
            .build();

    @Cleanup final InputStream fileStream = Files.newInputStream(file.toPath());

    sftpClient.uploadFile(sftpUser, sftpLocation, fileStream);

    final Path uploadedFilePath = remoteUploadDirectory.resolve(remoteFileName);
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
    String remoteFileName = String.format("keyUpload_%s", file.getName());
    final SftpUser sftpUser =
        SftpUser.builder().name(getSftpKeyConfig().get(SFTP_USER_NAME)).keyPair(keyPair).build();
    final SftpLocation sftpLocation =
        SftpLocation.builder()
            .host(getSftpKeyConfig().get(SFTP_HOST))
            .port(Integer.parseInt(getSftpKeyConfig().get(SFTP_PORT)))
            .path(
                String.format(
                    "%s/%s/%s", getSftpKeyConfig().get(SFTP_PATH), "upload", remoteFileName))
            .build();

    @Cleanup final InputStream fileStream = Files.newInputStream(file.toPath());

    sftpClient.uploadFile(sftpUser, sftpLocation, fileStream);

    final Path uploadedFilePath = remoteUploadDirectory.resolve(remoteFileName);
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
    String remoteFileName = String.format("pwDownload_%s", file.getName());
    final SftpUser sftpUser =
        SftpUser.builder()
            .name(getSftpPasswordConfig().get(SFTP_USER_NAME))
            .password(getSftpPasswordConfig().get(SFTP_USER_PASSWORD))
            .build();
    final SftpLocation sftpLocation =
        SftpLocation.builder()
            .host(getSftpPasswordConfig().get(SFTP_HOST))
            .port(Integer.parseInt(getSftpPasswordConfig().get(SFTP_PORT)))
            .path(
                String.format(
                    "%s/%s/%s", getSftpPasswordConfig().get(SFTP_PATH), "download", remoteFileName))
            .build();

    @Cleanup final InputStream fileToUpload = Files.newInputStream(file.toPath());
    Files.copy(
        fileToUpload,
        remoteDownloadDirectory.resolve(remoteFileName),
        StandardCopyOption.REPLACE_EXISTING);

    @Cleanup final InputStream source = Files.newInputStream(file.toPath());
    @Cleanup final InputStream target = sftpClient.downloadFile(sftpUser, sftpLocation);

    Assertions.assertTrue(
            IOUtils.contentEquals(source, target),
            String.format(
                    "File %s should have same content as file %s", file.toPath(), sftpLocation.getPath()));

  }

  @ParameterizedTest
  @SneakyThrows
  @ArgumentsSource(FilesProvider.class)
  void downloadFileWithKeyPair(File file) {
    String remoteFileName = String.format("pwDownload_%s", file.getName());
    final SftpUser sftpUser =
            SftpUser.builder().name(getSftpKeyConfig().get(SFTP_USER_NAME)).keyPair(keyPair).build();
    final SftpLocation sftpLocation =
            SftpLocation.builder()
                    .host(getSftpPasswordConfig().get(SFTP_HOST))
                    .port(Integer.parseInt(getSftpPasswordConfig().get(SFTP_PORT)))
                    .path(
                            String.format(
                                    "%s/%s/%s", getSftpPasswordConfig().get(SFTP_PATH), "download", remoteFileName))
                    .build();

    @Cleanup final InputStream fileToUpload = Files.newInputStream(file.toPath());
    Files.copy(
            fileToUpload,
            remoteDownloadDirectory.resolve(remoteFileName),
            StandardCopyOption.REPLACE_EXISTING);

    @Cleanup final InputStream source = Files.newInputStream(file.toPath());
    @Cleanup final InputStream target = sftpClient.downloadFile(sftpUser, sftpLocation);

    Assertions.assertTrue(
            IOUtils.contentEquals(source, target),
            String.format(
                    "File %s should have same content as file %s", file.toPath(), sftpLocation.getPath()));

  }

  private Map<String, String> getSftpPasswordConfig() {
    return Map.of(
        SFTP_HOST, "127.0.0.1",
        SFTP_PORT, sftpContainer.getFirstMappedPort().toString(),
        SFTP_PATH, "transfer",
        SFTP_USER_NAME, "user",
        SFTP_USER_PASSWORD, "password");
  }

  private Map<String, String> getSftpKeyConfig() {
    return Map.of(
        SFTP_HOST, "127.0.0.1",
        SFTP_PORT, sftpContainer.getFirstMappedPort().toString(),
        SFTP_PATH, "transfer",
        SFTP_USER_NAME, "user");
  }

  @SneakyThrows
  private static KeyPair generateKeyPair() {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    return keyPairGenerator.generateKeyPair();
  }

  @NoArgsConstructor
  private static class FilesProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          Arguments.of(get1KBFile()), Arguments.of(get1MBFile()), Arguments.of(get2MBFile()));
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
      Path path = localUploadAndGeneratorDirectory.resolve(String.format("%s.bin", byteSize));
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
