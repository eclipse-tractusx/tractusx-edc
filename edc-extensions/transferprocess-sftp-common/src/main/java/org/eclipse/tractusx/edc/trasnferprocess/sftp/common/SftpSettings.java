package org.eclipse.tractusx.edc.trasnferprocess.sftp.common;

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.EdcSetting;

public interface SftpSettings {
  @EdcSetting(required = true)
  String SFTP_HOST = "edc.transfer.sftp.location.host";

  @EdcSetting(required = true)
  String SFTP_PORT = "edc.transfer.sftp.location.port";

  @EdcSetting(required = true)
  String SFTP_PATH = "edc.transfer.sftp.location.path";

  @EdcSetting(required = true)
  String SFTP_USER_NAME = "edc.transfer.sftp.user.name";

  @EdcSetting String SFTP_USER_PASSWORD = "edc.transfer.sftp.user.password";

  @EdcSetting String SFTP_USER_KEY_PATH = "edc.transfer.sftp.user.key.path";

  @EdcSetting String SFTP_USER_KEY_PASSPHRASE = "edc.transfer.sftp.user.key.passphrase";
}
