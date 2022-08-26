package net.catenax.edc.transferprocess.sftp.provisioner;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConfigBackedSftpLocationFactory implements SftpLocationFactory {
    @NonNull
    private final String sftpHost;
    @NonNull
    private final Integer sftpPort;
    @NonNull
    private final String sftpPath;

    @Override
    public SftpLocation createSftpLocation(String transferProcessId) {
        return SftpLocation.builder().host(sftpHost).port(sftpPort).path(sftpPath).build();
    }
}
