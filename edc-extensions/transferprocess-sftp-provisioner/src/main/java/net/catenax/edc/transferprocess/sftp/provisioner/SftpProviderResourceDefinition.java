package net.catenax.edc.transferprocess.sftp.provisioner;

import lombok.Getter;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;

public class SftpProviderResourceDefinition extends ResourceDefinition {
    @Getter
    private String dataAddressType;
}
