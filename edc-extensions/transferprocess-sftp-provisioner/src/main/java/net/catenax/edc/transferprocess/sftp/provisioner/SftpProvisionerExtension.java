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

import net.catenax.edc.trasnferprocess.sftp.common.SftpLocationFactory;
import net.catenax.edc.trasnferprocess.sftp.common.SftpProvider;
import net.catenax.edc.trasnferprocess.sftp.common.SftpSettings;
import net.catenax.edc.trasnferprocess.sftp.common.SftpUserFactory;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.eclipse.dataspaceconnector.core.policy.engine.PolicyEngineImpl;
import org.eclipse.dataspaceconnector.core.policy.engine.RuleBindingRegistryImpl;
import org.eclipse.dataspaceconnector.core.policy.engine.ScopeFilter;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.engine.PolicyEngine;
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
public class SftpProvisionerExtension implements ServiceExtension, SftpSettings {


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
