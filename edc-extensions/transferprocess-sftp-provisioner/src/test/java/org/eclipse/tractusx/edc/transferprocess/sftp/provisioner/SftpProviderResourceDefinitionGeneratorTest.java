/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.transferprocess.sftp.provisioner;


import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import static org.eclipse.tractusx.edc.transferprocess.sftp.provisioner.NoOpSftpProvisioner.DATA_ADDRESS_TYPE;
import static org.eclipse.tractusx.edc.transferprocess.sftp.provisioner.NoOpSftpProvisioner.PROVIDER_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


class SftpProviderResourceDefinitionGeneratorTest {
    private final SftpProviderResourceDefinitionGenerator generator =
            new SftpProviderResourceDefinitionGenerator();

    static KeyPair generateKeyPair() {
        try {
            var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void generate_successful() throws NoSuchAlgorithmException {
        var name = "name";
        var password = "password";
        var keyPair = generateKeyPair();
        var host = "host";
        var port = 22;
        var path = "path";

        var dataRequest = DataRequest.Builder.newInstance().destinationType(DATA_ADDRESS_TYPE).build();
        var sftpUser = SftpUser.Builder.newInstance().name(name).password(password).keyPair(keyPair).build();
        var sftpLocation =
                SftpLocation.Builder.newInstance().host(host).port(port).path(path).build();
        final DataAddress dataAddress = SftpDataAddress.Builder.newInstance().sftpUser(sftpUser).sftpLocation(sftpLocation).build();
        var policy = Policy.Builder.newInstance().build();

        var resourceDefinition =
                (SftpProviderResourceDefinition) generator.generate(dataRequest, dataAddress, policy);

        assertNotNull(resourceDefinition);
        var sftpDataAddress = resourceDefinition.getSftpDataAddress();

        assertEquals(PROVIDER_TYPE, resourceDefinition.getProviderType());
        assertEquals(host, sftpDataAddress.getSftpLocation().getHost());
        assertEquals(port, sftpDataAddress.getSftpLocation().getPort());
        assertEquals(path, sftpDataAddress.getSftpLocation().getPath());
        assertEquals(name, sftpDataAddress.getSftpUser().getName());
        assertEquals(password, sftpDataAddress.getSftpUser().getPassword());

    }

    @Test
    void generate_wrongDataAddressType() {
        var dataRequest =
                DataRequest.Builder.newInstance().destinationType(DATA_ADDRESS_TYPE).build();
        var dataAddress = DataAddress.Builder.newInstance().type("wrong").build();
        var policy = Policy.Builder.newInstance().build();

        var resourceDefinition =
                (SftpProviderResourceDefinition) generator.generate(dataRequest, dataAddress, policy);

        assertNull(resourceDefinition);
    }
}
