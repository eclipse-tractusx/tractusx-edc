/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial Test
 *
 */

package net.catnax.edc.transferprocess.sftp.client;

import lombok.Getter;
import lombok.SneakyThrows;
import net.catenax.edc.transferprocess.sftp.client.SftpClient;
import net.catenax.edc.transferprocess.sftp.provisioner.SftpLocation;
import net.catenax.edc.transferprocess.sftp.provisioner.SftpProviderResourceDefinition;
import net.catenax.edc.transferprocess.sftp.provisioner.SftpUser;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyArchive;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.junit.ClassRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static net.catenax.edc.transferprocess.sftp.provisioner.SftpProvisionerExtension.SFTP_HOST;
import static net.catenax.edc.transferprocess.sftp.provisioner.SftpProvisionerExtension.SFTP_PATH;
import static net.catenax.edc.transferprocess.sftp.provisioner.SftpProvisionerExtension.SFTP_PORT;
import static net.catenax.edc.transferprocess.sftp.provisioner.SftpProvisionerExtension.SFTP_USER_KEY;
import static net.catenax.edc.transferprocess.sftp.provisioner.SftpProvisionerExtension.SFTP_USER_NAME;

@Testcontainers
@ExtendWith(EdcExtension.class)
public class SftpClientIT {
    static final String DOCKER_IMAGE_NAME = "atmoz/sftp:alpine-3.6";
    static final Map<String, String> DOCKER_ENV = Map.of("SFTP_USERS", "user:key:::upload");

    private final TestExtension testExtension = new TestExtension();

    @Container
    @ClassRule
    private static final GenericContainer<?> sftpContainer =
            new GenericContainer<>(DockerImageName.parse(DOCKER_IMAGE_NAME))
                    .withEnv(DOCKER_ENV)
                    .withExposedPorts(22)
                    .waitingFor(Wait.forListeningPort());

    @BeforeEach
    void setup(EdcExtension extension) {
        extension.setConfiguration(getSftpConfig());
        extension.registerSystemExtension(ServiceExtension.class, testExtension);


    }

    @Test
    @SneakyThrows
    void uploadFile() {
        SftpProviderResourceDefinition resourceDefinition = new SftpProviderResourceDefinition("sftp");
        Policy policy = Policy.Builder.newInstance().build();
        testExtension.getProvisionManager().provision(List.of(resourceDefinition), policy);

        SftpUser sftpUser = SftpUser.builder()
                .name(getSftpConfig().get(SFTP_USER_NAME))
                .key(getSftpConfig().get(SFTP_USER_KEY).getBytes(StandardCharsets.UTF_8))
                .build();
        SftpLocation sftpLocation = SftpLocation.builder()
                .host(getSftpConfig().get(SFTP_HOST))
                .port(Integer.parseInt(getSftpConfig().get(SFTP_PORT)))
                .path(getSftpConfig().get(SFTP_PATH))
                .build();
        InputStream testFile = getClass().getClassLoader().getResourceAsStream("testFile");
        testExtension.getSftpClient().uploadFile(sftpUser, sftpLocation, testFile);
    }

    private Map<String, String> getSftpConfig() {
        return Map.of(
                SFTP_HOST, "localhost",
                SFTP_PORT, sftpContainer.getFirstMappedPort().toString(),
                SFTP_PATH, "upload",
                SFTP_USER_NAME, "user",
                SFTP_USER_KEY, "key");
    }

    @Getter
    @Requires({SftpClient.class, ProvisionManager.class})
    @Provides(PolicyArchive.class)
    private static class TestExtension implements ServiceExtension {
        private SftpClient sftpClient;
        private ProvisionManager provisionManager;
        private final PolicyArchive policyArchive = Mockito.mock(PolicyArchive.class);

        @Override
        public void initialize(ServiceExtensionContext context) {
            sftpClient = context.getService(SftpClient.class);
            provisionManager = context.getService(ProvisionManager.class);
            context.registerService(PolicyArchive.class, policyArchive);
        }
    }
}
