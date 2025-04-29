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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * File-based implementation of the {@link TargetNodeDirectory} that returns a static {@code List<TargetNode>} that are
 * serialized as JSON and stored in a file.
 */
class FileBasedTargetNodeDirectory implements TargetNodeDirectory {

    private static final TypeReference<List<TargetNode>> LIST_TYPE = new TypeReference<>() {
    };
    private final File nodeFile;
    private final Monitor monitor;
    private final ObjectMapper objectMapper;
    private List<TargetNode> nodes;

    FileBasedTargetNodeDirectory(File nodeFile, Monitor monitor, ObjectMapper objectMapper) {

        this.nodeFile = nodeFile;
        this.monitor = monitor;
        this.objectMapper = objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }

    @Override
    public List<TargetNode> getAll() {

        if (nodes == null) {
            try {
                nodes = objectMapper.readValue(nodeFile, LIST_TYPE);
            } catch (IOException e) {
                throw new EdcException(e);
            }
        }
        return nodes;

    }

    @Override
    public void insert(TargetNode targetNode) {
        monitor.warning("Inserting nodes into the file-based TargetNodeDirectory is not supported.");
    }

    @Override
    public TargetNode remove(String s) {
        monitor.warning("Deleting nodes into the file-based TargetNodeDirectory is not supported.");
        return null;
    }
}
