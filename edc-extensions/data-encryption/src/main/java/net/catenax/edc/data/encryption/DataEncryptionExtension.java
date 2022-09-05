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
package net.catenax.edc.data.encryption;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import net.catenax.edc.data.encryption.encrypter.AesDataEncrypterConfiguration;
import net.catenax.edc.data.encryption.encrypter.DataEncrypterFactory;
import net.catenax.edc.data.encryption.key.AesKey;
import net.catenax.edc.data.encryption.key.CryptoKeyFactory;
import net.catenax.edc.data.encryption.key.CryptoKeyFactoryImpl;
import net.catenax.edc.data.encryption.provider.AesKeyProvider;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;

@Provides({DataEncrypter.class})
@Requires({Vault.class})
public class DataEncryptionExtension implements ServiceExtension {

  public static final String NAME = "Data Encryption Extension";

  @EdcSetting public static final String ENCRYPTION_KEY_SET = "edc.data.encryption.keys.alias";

  @EdcSetting public static final String ENCRYPTION_ALGORITHM = "edc.data.encryption.algorithm";
  public static final String ENCRYPTION_ALGORITHM_DEFAULT = DataEncrypterFactory.AES_ALGORITHM;

  @EdcSetting public static final String CACHING_ENABLED = "edc.data.encryption.caching.enabled";
  public static final boolean CACHING_ENABLED_DEFAULT = false;

  @EdcSetting public static final String CACHING_SECONDS = "edc.data.encryption.caching.seconds";
  public static final int CACHING_SECONDS_DEFAULT = 3600;

  private static final CryptoKeyFactory cryptoKeyFactory = new CryptoKeyFactoryImpl();

  private Monitor monitor;
  private Vault vault;
  private ServiceExtensionContext context;

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public void start() {

    final String algorithm = context.getSetting(ENCRYPTION_ALGORITHM, ENCRYPTION_ALGORITHM_DEFAULT);

    if (DataEncrypterFactory.NONE.equalsIgnoreCase(algorithm)) {
      return; // no start-up checks for NONE algorithm
    }

    if (DataEncrypterFactory.AES_ALGORITHM.equals(algorithm)) {

      final AesDataEncrypterConfiguration configuration = createAesConfiguration(context);
      final String keyAlias = configuration.getKeySetAlias();
      final String keySecret = vault.resolveSecret(keyAlias);
      if (keySecret == null || keySecret.isEmpty()) {
        throw new EdcException(NAME + ": No vault key secret found for alias " + keyAlias);
      }

      try {
        final AesKeyProvider aesKeyProvider = new AesKeyProvider(vault, keyAlias, cryptoKeyFactory);
        final List<AesKey> keys = aesKeyProvider.getDecryptionKeySet().collect(Collectors.toList());
        monitor.debug(
            String.format(
                "Started " + NAME + ": Found %s registered AES keys in vault.", keys.size()));
      } catch (Exception e) {
        throw new EdcException(
            NAME + ": AES keys from vault must be comma separated and Base64 encoded.", e);
      }
    }
  }

  @Override
  public void initialize(ServiceExtensionContext context) {
    this.context = context;
    this.monitor = context.getMonitor();
    this.vault = context.getService(Vault.class);
    final DataEncrypterFactory factory = new DataEncrypterFactory(vault, monitor, cryptoKeyFactory);

    final DataEncrypter dataEncrypter;
    final String algorithm = context.getSetting(ENCRYPTION_ALGORITHM, ENCRYPTION_ALGORITHM_DEFAULT);
    if (DataEncrypterFactory.NONE.equalsIgnoreCase(algorithm)) {
      dataEncrypter = factory.createNoneEncrypter();
    } else if (DataEncrypterFactory.AES_ALGORITHM.equalsIgnoreCase(algorithm)) {
      final AesDataEncrypterConfiguration configuration = createAesConfiguration(context);
      dataEncrypter = factory.createAesEncrypter(configuration);
    } else {
      final String msg =
          String.format(
              DataEncryptionExtension.NAME
                  + ": Unsupported encryption algorithm '%s'. Supported algorithms are '%s',  '%s'.",
              algorithm,
              DataEncrypterFactory.AES_ALGORITHM,
              DataEncrypterFactory.NONE);
      throw new EdcException(msg);
    }

    context.registerService(DataEncrypter.class, dataEncrypter);
  }

  private static AesDataEncrypterConfiguration createAesConfiguration(
      ServiceExtensionContext context) {
    final String key = context.getSetting(ENCRYPTION_KEY_SET, null);
    if (key == null) {
      throw new EdcException(NAME + ": Missing setting " + ENCRYPTION_KEY_SET);
    }

    final boolean cachingEnabled = context.getSetting(CACHING_ENABLED, CACHING_ENABLED_DEFAULT);
    final int cachingSeconds = context.getSetting(CACHING_SECONDS, CACHING_SECONDS_DEFAULT);

    return new AesDataEncrypterConfiguration(
        key, cachingEnabled, Duration.ofSeconds(cachingSeconds));
  }
}
