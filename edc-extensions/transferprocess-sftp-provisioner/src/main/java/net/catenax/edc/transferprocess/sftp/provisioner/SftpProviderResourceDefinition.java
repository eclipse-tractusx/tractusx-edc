package net.catenax.edc.transferprocess.sftp.provisioner;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;

@Getter
@RequiredArgsConstructor
public class SftpProviderResourceDefinition extends ResourceDefinition {
    @NonNull
    private String dataAddressType;
}
