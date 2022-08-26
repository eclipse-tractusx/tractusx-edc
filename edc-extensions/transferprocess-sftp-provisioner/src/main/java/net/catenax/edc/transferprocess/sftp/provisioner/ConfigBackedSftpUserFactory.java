package net.catenax.edc.transferprocess.sftp.provisioner;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConfigBackedSftpUserFactory implements SftpUserFactory {
    @NonNull
    private final String sftpUserName;
    @NonNull
    private final byte[] sftpUserKey;

    @Override
    public SftpUser createSftpUser(String transferProcessId) {
        return SftpUser.builder().name(sftpUserName).key(sftpUserKey).build();
    }
}
