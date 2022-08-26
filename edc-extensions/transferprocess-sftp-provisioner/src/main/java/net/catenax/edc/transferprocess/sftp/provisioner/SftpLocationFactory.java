package net.catenax.edc.transferprocess.sftp.provisioner;

public interface SftpLocationFactory {
    SftpLocation createSftpLocation(String transferProcessId);
}
