package net.catenax.edc.transferprocess.sftp.provisioner;

import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.security.KeyPair;

@Builder
public class ConfigBackedSftpUserFactory implements SftpUserFactory {
    @NonNull
    private final String sftpUserName;
    private final String sftpUserPassword;
    private final KeyPair sftpUserKeyPair;

    @Override
    public SftpUser createSftpUser(String transferProcessId) {
        if (sftpUserKeyPair != null) {
            return SftpUser.builder().name(sftpUserName).keyPair(sftpUserKeyPair).build();
        }
        if (sftpUserPassword != null) {
            return SftpUser.builder().name(sftpUserName).password(sftpUserPassword).build();
        }
        throw new EdcException(String.format("No auth method provided for SftpUser %s", sftpUserName));
    }
}
