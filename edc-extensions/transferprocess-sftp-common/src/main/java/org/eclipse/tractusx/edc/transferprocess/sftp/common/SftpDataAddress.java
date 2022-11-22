package org.eclipse.tractusx.edc.transferprocess.sftp.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
public class SftpDataAddress extends DataAddress {
  @NonNull private String sftpUserName;
  @ToString.Exclude private String sftpUserPassword;
  @ToString.Exclude private byte[] sftpUserPrivateKey;
  @NonNull private String sftpLocationHost;
  @NonNull private Integer sftpLocationPort;
  @NonNull private String sftpLocationPath;
}
