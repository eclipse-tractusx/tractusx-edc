package net.catenax.edc.transferprocess.sftp.provisioner;

public interface SftpUserFactory {
    SftpUser createSftpUser(String transferProcessId);
}
