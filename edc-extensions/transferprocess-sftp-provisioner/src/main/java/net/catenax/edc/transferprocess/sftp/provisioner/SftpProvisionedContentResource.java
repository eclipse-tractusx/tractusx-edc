package net.catenax.edc.transferprocess.sftp.provisioner;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedContentResource;

@Getter
@RequiredArgsConstructor
public class SftpProvisionedContentResource extends ProvisionedContentResource {
    @NonNull
    private SftpUser sftpUser;
    @NonNull
    private SftpLocation sftpLocation;
    @NonNull
    private String transferProcessId;
}
