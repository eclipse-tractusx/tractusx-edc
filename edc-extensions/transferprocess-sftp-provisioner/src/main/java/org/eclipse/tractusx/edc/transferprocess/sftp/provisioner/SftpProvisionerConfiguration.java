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

/**
 * Configuration to create a resource definition and provisioner pair for sftp data transfer.
 */
public class SftpProvisionerConfiguration {

    private String name;

    private final ProvisionerType provisionerType = ProvisionerType.PROVIDER;

    private String dataAddressType;
    private String policyScope;
    private URL endpoint;

    public enum ProvisionerType {
        CONSUMER,
        PROVIDER
    }
}
