package net.catenax.edc.transferprocess.sftp.client;

import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.catenax.edc.transferprocess.sftp.provisioner.SftpLocation;
import net.catenax.edc.transferprocess.sftp.provisioner.SftpUser;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;

import java.io.InputStream;

@Builder
public class SftpPart implements DataSource.Part {
    @NonNull
    private final SftpUser sftpUser;
    @NonNull
    private final SftpLocation sftpLocation;
    @NonNull
    private final SftpClient sftpClient;
    @Override
    public String name() {
        return sftpLocation.getPath();
    }

    @Override
    @SneakyThrows
    public InputStream openStream() {
        return sftpClient.downloadFile(sftpUser, sftpLocation);
    }
}
