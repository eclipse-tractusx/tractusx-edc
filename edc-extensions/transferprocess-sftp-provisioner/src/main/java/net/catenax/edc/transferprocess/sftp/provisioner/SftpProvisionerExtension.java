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
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package net.catenax.edc.transferprocess.sftp.provisioner;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.eclipse.dataspaceconnector.core.base.policy.PolicyEngineImpl;
import org.eclipse.dataspaceconnector.core.base.policy.RuleBindingRegistryImpl;
import org.eclipse.dataspaceconnector.core.base.policy.ScopeFilter;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;

import java.io.FileReader;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import static net.catenax.edc.transferprocess.sftp.provisioner.SftpProvisionerConfiguration.ProvisionerType.PROVIDER;

@Provides(SftpProvisioner.class)
public class SftpProvisionerExtension implements ServiceExtension {

    @EdcSetting(required = true)
    public static final String SFTP_HOST = "edc.transfer.sftp.location.host";

    @EdcSetting(required = true)
    public static final String SFTP_PORT = "edc.transfer.sftp.location.port";

    @EdcSetting(required = true)
    public static final String SFTP_PATH = "edc.transfer.sftp.location.path";

    @EdcSetting(required = true)
    public static final String SFTP_USER_NAME = "edc.transfer.sftp.user.name";

    @EdcSetting
    public static final String SFTP_USER_PASSWORD = "edc.transfer.sftp.user.password";

    @EdcSetting
    public static final String SFTP_USER_KEY_PATH = "edc.transfer.sftp.user.key.path";

    @EdcSetting
    public static final String SFTP_USER_KEY_PASSPHRASE = "edc.transfer.sftp.user.key.passphrase";

    ProvisionManager provisionManager;
    ResourceManifestGenerator manifestGenerator;
    @Inject
    Monitor monitor;

    @Override
    public String name() {
        return "Sftp Provisioner";
    }

    private SftpProvisioner sftpProvisioner;

    @Override
    public void initialize(ServiceExtensionContext context) {
        provisionManager = context.getService(ProvisionManager.class);
        manifestGenerator = context.getService(ResourceManifestGenerator.class);

        PolicyEngine policyEngine = new PolicyEngineImpl(new ScopeFilter(new RuleBindingRegistryImpl()));

        final String sftpHost = context.getSetting(SFTP_HOST, "localhost");
        final Integer sftpPort = context.getSetting(SFTP_PORT, 22);
        final String sftpPath = context.getSetting(SFTP_PATH, "test");
        SftpLocationFactory locationFactory = new ConfigBackedSftpLocationFactory(sftpHost, sftpPort, sftpPath);

        final String sftpName = context.getSetting(SFTP_USER_NAME, "user");
        final String sftpKeyPath = context.getSetting(SFTP_USER_KEY_PATH, null);
        final String sftpKeyPassphrase = context.getSetting(SFTP_USER_KEY_PASSPHRASE, null);
        final KeyPair sftpKeyPair = readPrivateKey(sftpKeyPath, sftpKeyPassphrase);
        final String sftpPassword = context.getSetting(SFTP_USER_PASSWORD, null);
        SftpUserFactory userFactory = ConfigBackedSftpUserFactory.builder().sftpUserName(sftpName).sftpUserKeyPair(sftpKeyPair).sftpUserPassword(sftpPassword).build();

        SftpProvider sftpProvider = new NoopSftpProvider();

        var configurations = SftpConfigParser.parseConfigurations(context.getConfig());
        for (var configuration : configurations) {
            sftpProvisioner = new SftpProvisioner(policyEngine, locationFactory, userFactory, sftpProvider);

            if (configuration.getProvisionerType() == PROVIDER) {
                var generator = new SftpProviderResourceDefinitionGenerator(configuration.getDataAddressType());
                manifestGenerator.registerGenerator(generator);
                monitor.info(String.format("Registering provider provisioner: %s [%s]", configuration.getName(), configuration.getEndpoint()));
            } else {
                monitor.warning(String.format("Client-side provisioning not yet supported by the %s. Skipping configuration for %s", name(), configuration.getName()));
            }

            provisionManager.register(sftpProvisioner);
        }


        context.getMonitor().info("SftpProvisionerExtension: authentication/initialization complete.");
    }

    private KeyPair readPrivateKey(String sftpKeyPath, String sftpKeyPassphrase) {
        if (sftpKeyPath == null) {
            return null;
        }

        try (FileReader keyReader = new FileReader(sftpKeyPath)) {
            PEMParser pemParser = new PEMParser(keyReader);
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

            PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(pemParser.readObject());
            PrivateKey privateKey = converter.getPrivateKey(privateKeyInfo);
            RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) privateKey;

            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(rsaPrivateKey.getModulus(), rsaPrivateKey.getPublicExponent());
            KeyFactory factory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = factory.generatePublic(publicKeySpec);

            return new KeyPair(publicKey, privateKey);
        } catch (InvalidKeySpecException | IOException | NoSuchAlgorithmException e) {
            throw new EdcException(String.format("Unable for read key stored at %s", sftpKeyPath));
        }
    }
}
