package org.eclipse.tractusx.edc.transferprocess.sftp.common;

import java.nio.charset.StandardCharsets;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

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

  @Getter private static final String connectionType = "sftp";
  @NonNull private final SftpUser sftpUser;
  @NonNull private final SftpLocation sftpLocation;

  public static SftpDataAddress fromDataAddress(DataAddress dataAddress) {
    if (dataAddress instanceof SftpDataAddress) {
      return (SftpDataAddress) dataAddress;
    }

    SftpUser sftpUser =
        SftpUser.builder()
            .name(dataAddress.getProperty(USER_NAME))
            .password(dataAddress.getProperty(USER_PASSWORD))
            .keyPair(
                SftpUserKeyPairGenerator.getKeyPairFromPrivateKey(
                    dataAddress.getProperty(USER_PRIVATE_KEY).getBytes(StandardCharsets.UTF_8),
                    dataAddress.getProperty(USER_NAME)))
            .build();

    SftpLocation sftpLocation =
        SftpLocation.builder()
            .host(dataAddress.getProperty(LOCATION_HOST))
            .port(Integer.getInteger(dataAddress.getProperty(LOCATION_PORT)))
            .path(dataAddress.getProperty(LOCATION_PATH))
            .build();

    return SftpDataAddress.builder().sftpUser(sftpUser).sftpLocation(sftpLocation).build();
  }
}
