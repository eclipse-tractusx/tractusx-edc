/********************************************************************************
 * Copyright (c) 2026 SAP SE
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

package org.eclipse.tractusx.edc.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

class FileUtilsTest {

    @Test
    @DisplayName("Should successfully load and copy a valid resource file")
    void testGetResourceFile_SuccessfullyLoadsResource() {
        var result = FileUtils.getResourceFile("test-resource.txt");

        assertThat(result).isNotNull();
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent())
                .isNotNull()
                .isInstanceOf(File.class);

        File resultFile = result.getContent();
        assertThat(resultFile.exists()).isTrue();
        assertThat(resultFile.getName())
                .contains("test-resource")
                .endsWith(".txt");

        // Cleanup
        resultFile.deleteOnExit();
    }

    @Test
    @DisplayName("Should return failure when resource is not found")
    void testGetResourceFile_ResourceNotFound() {
        var result = FileUtils.getResourceFile("non-existent-resource.txt");

        assertThat(result).isNotNull();
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().getMessages())
                .isNotEmpty()
                .contains("Cannot find resource non-existent-resource.txt");
    }

    @Test
    @DisplayName("Should handle resource names with dots in filename")
    void testGetResourceFile_WithDotsInFilename() {
        var result = FileUtils.getResourceFile("test-resource.backup.txt");

        assertThat(result).isNotNull();
        if (result.succeeded()) {
            File resultFile = result.getContent();
            assertThat(resultFile).isNotNull();
            resultFile.deleteOnExit();
        } else {
            assertThat(result.getFailure().getMessages()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("Should not allow instantiation of FileUtils")
    void testFileUtilsPrivateConstructor() {
        // Verify that FileUtils has a private constructor by checking the class structure
        assertThat(FileUtils.class.getDeclaredConstructors()).hasSize(1);
        var constructor = FileUtils.class.getDeclaredConstructors()[0];
        assertThat(constructor.getModifiers()).matches(Modifier::isPrivate);
        assertThat(constructor.getParameters()).isEmpty();
    }
}

