/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

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
