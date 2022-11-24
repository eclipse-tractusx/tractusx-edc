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

import java.net.URL;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/** Configuration to create a resource definition and provisioner pair for sftp data transfer. */
@Getter
@Builder
public class SftpProvisionerConfiguration {

  @NonNull private final String name;

  @NonNull @Builder.Default
  private final ProvisionerType provisionerType = ProvisionerType.PROVIDER;

  @NonNull private final String dataAddressType;
  @NonNull private final String policyScope;
  @NonNull private final URL endpoint;

  public enum ProvisionerType {
    CONSUMER,
    PROVIDER
  }
}
