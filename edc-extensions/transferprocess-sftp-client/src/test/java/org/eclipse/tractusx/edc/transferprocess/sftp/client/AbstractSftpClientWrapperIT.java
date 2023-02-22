package org.eclipse.tractusx.edc.transferprocess.sftp.client;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
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
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
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
  @Container @ClassRule private static final GenericContainer<?> sftpContainer;

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

    sftpContainer =
        new GenericContainer<>(DockerImageName.parse(DOCKER_IMAGE_NAME))
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
    if (Files.exists(keyDirectory)) {
      Files.walk(keyDirectory)
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    }
  }

  protected SftpUser getPasswordUser() {
    return SftpUser.builder().name("user").password("password").build();
  }

  protected SftpUser getKeyPairUser() {
    return SftpUser.builder().name("user").keyPair(keyPair).build();
  }

  protected SftpLocation getSftpLocation(String path) {
    return SftpLocation.builder()
        .host("127.0.0.1")
        .port(sftpContainer.getFirstMappedPort())
        .path(path)
        .build();
  }

  protected SftpClientWrapper getSftpClient(SftpLocation location, SftpUser sftpUser) {
    SftpClientConfig config =
        SftpClientConfig.builder()
            .sftpLocation(location)
            .sftpUser(sftpUser)
            .hostVerification(false)
            .build();
    return new SftpClientWrapperImpl(config);
  }

  @NoArgsConstructor
  protected static class FilesProvider implements ArgumentsProvider {
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

  @SneakyThrows
  private static KeyPair generateKeyPair() {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    return keyPairGenerator.generateKeyPair();
  }
}
