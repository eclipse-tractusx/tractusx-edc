/********************************************************************************
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.edc.transferprocess.sftp.provisioner;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceDefinition;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;

public class SftpProviderResourceDefinition extends ResourceDefinition {
    private final String providerType;
    private final SftpDataAddress sftpDataAddress;

    public SftpProviderResourceDefinition(String providerType, SftpDataAddress sftpDataAddress) {
        this.providerType = providerType;
        this.sftpDataAddress = sftpDataAddress;
    }


    public String getProviderType() {
        return providerType;
    }

    public SftpDataAddress getSftpDataAddress() {
        return sftpDataAddress;
    }

    @Override
    public <R extends ResourceDefinition, B extends Builder<R, B>> B toBuilder() {
        return null;
    }
}
