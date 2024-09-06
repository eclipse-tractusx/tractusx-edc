/********************************************************************************
 * Copyright (c) 2020,2021 Microsoft Corporation
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

package org.eclipse.edc.connector.provision.azure.blob;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectStorageResourceDefinitionTest {

    @ParameterizedTest
    @ValueSource(strings = { "test-folder" })
    @NullAndEmptySource
    void toBuilder_verifyEqualResourceDefinition(String folder) {
        var definition = ObjectStorageResourceDefinition.Builder.newInstance()
                .id("id")
                .transferProcessId("tp-id")
                .accountName("account")
                .containerName("container")
                .folderName(folder)
                .build();
        var builder = definition.toBuilder();
        var rebuiltDefinition = builder.build();

        assertThat(rebuiltDefinition).usingRecursiveComparison().isEqualTo(definition);
    }

}
