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

import org.eclipse.edc.connector.transfer.spi.provision.ProviderResourceDefinitionGenerator;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.EdcSftpException;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;
import org.jetbrains.annotations.Nullable;


public class SftpProviderResourceDefinitionGenerator implements ProviderResourceDefinitionGenerator {

    @Override
    public @Nullable ResourceDefinition generate(TransferProcess transferProcess, DataAddress assetAddress, Policy policy) {
        try {
            var sftpDataAddress = SftpDataAddress.fromDataAddress(assetAddress);
            return new SftpProviderResourceDefinition(NoOpSftpProvisioner.PROVIDER_TYPE, sftpDataAddress);
        } catch (EdcSftpException e) {
            return null;
        }
    }

    @Override
    public boolean canGenerate(TransferProcess transferProcess, DataAddress dataAddress, Policy policy) {
        try {
            SftpDataAddress.fromDataAddress(dataAddress);
        } catch (EdcSftpException e) {
            return false;
        }
        return true;
    }
}
