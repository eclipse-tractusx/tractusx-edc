/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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
 */

package org.eclipse.tractusx.edc.federatedcatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileBasedTargetNodeDirectoryTest {

    @Test
    void getAll_hasEntries() {
        var nodeDir = new FileBasedTargetNodeDirectory(TestUtils.getFileFromResourceName("nodes.json"), mock(), new ObjectMapper());
        assertThat(nodeDir.getAll()).hasSize(2);
    }

    @Test
    void getAll_fileNotExist() {
        var nodeDir = new FileBasedTargetNodeDirectory(new File("not-exist.json"), mock(), new ObjectMapper());
        assertThatThrownBy(nodeDir::getAll)
                .isInstanceOf(EdcException.class)
                .hasRootCauseInstanceOf(FileNotFoundException.class);
    }

    @Test
    void insert() {
        var monitor = mock(Monitor.class);
        when(monitor.withPrefix(anyString())).thenReturn(monitor);
        var nodeDir = new FileBasedTargetNodeDirectory(new File("not-exist.json"), monitor, new ObjectMapper());

        assertThatNoException().isThrownBy(() -> nodeDir.insert(new TargetNode("foo", "bar", "https://foobar.com", List.of())));
        verify(monitor).warning("Inserting nodes into the file-based TargetNodeDirectory is not supported.");
    }
}