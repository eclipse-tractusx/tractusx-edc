/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Mercedes Benz Tech Innovation - adapt to SFTP
 *
 */

package org.eclipse.tractusx.edc.transferprocess.sftp.provisioner;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.EdcSetting;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.system.configuration.Config;

/** Parses provisioner configuration. */
public class SftpConfigParser {
  private static final String DEFAULT_POLICY_SCOPE = "sftp.provisioner";

  private static final String CONFIG_PREFIX = "provisioner.sftp";

  private static final String HTTP_PROVISIONER_ENTRIES = CONFIG_PREFIX + ".entries";

  @EdcSetting(required = true)
  private static final String PROVISIONER_TYPE = "provisioner.type";

  @EdcSetting(required = true)
  private static final String DATA_ADDRESS_TYPE = "data.address.type";

  @EdcSetting(required = true)
  private static final String ENDPOINT_URL = "endpoint";

  @EdcSetting private static final String POLICY_SCOPE = "policy.scope";

  private SftpConfigParser() {}

  /** Parses the runtime configuration source, returning a provisioner configuration. */
  static List<SftpProvisionerConfiguration> parseConfigurations(Config root) {

    var configurations = root.getConfig(HTTP_PROVISIONER_ENTRIES);

    return configurations
        .partition()
        .map(
            config -> {
              final String provisionerName = config.currentNode();

              final SftpProvisionerConfiguration.ProvisionerType provisionerType =
                  parseProvisionerType(config, provisionerName);

              final URL endpoint = parseEndpoint(config, provisionerName);

              final String policyScope = config.getString(POLICY_SCOPE, DEFAULT_POLICY_SCOPE);

              final String dataAddressType = config.getString(DATA_ADDRESS_TYPE);

              return SftpProvisionerConfiguration.builder()
                  .name(provisionerName)
                  .provisionerType(provisionerType)
                  .dataAddressType(dataAddressType)
                  .policyScope(policyScope)
                  .endpoint(endpoint)
                  .build();
            })
        .collect(toList());
  }

  private static URL parseEndpoint(Config config, String provisionerName) {
    final String endpoint = config.getString(ENDPOINT_URL);
    try {
      return new URL(endpoint);
    } catch (MalformedURLException e) {
      throw new EdcException("Invalid endpoint URL for SFTP provisioner: " + provisionerName, e);
    }
  }

  private static SftpProvisionerConfiguration.ProvisionerType parseProvisionerType(
      Config config, String provisionerName) {
    final String typeValue =
        config.getString(
            PROVISIONER_TYPE, SftpProvisionerConfiguration.ProvisionerType.PROVIDER.name());
    try {
      return SftpProvisionerConfiguration.ProvisionerType.valueOf(typeValue.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new EdcException(
          format("Invalid provisioner type specified for %s: %s", provisionerName, typeValue));
    }
  }
}
