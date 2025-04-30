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

import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static java.util.Optional.ofNullable;
import static org.eclipse.tractusx.edc.federatedcatalog.FederatedCatalogExtension.NAME;


@Extension(value = NAME)
public class FederatedCatalogExtension implements ServiceExtension {

    public static final String NAME = "Tractus-X Federated Catalog Extension";

    @Setting(value = "File path to a JSON file containing TargetNode entries for the Federated Catalog Crawler")
    public static final String NODE_LIST_FILE = "tx.edc.catalog.node.list.file";

    @Inject
    private TypeManager typeManager;


    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public TargetNodeDirectory createFileBasedNodeDirectory(ServiceExtensionContext context) {
        return ofNullable(context.getConfig().getString(NODE_LIST_FILE, null))
                .map(File::new)
                .map(f -> (TargetNodeDirectory) new FileBasedTargetNodeDirectory(f, context.getMonitor(), typeManager.getMapper()))
                .orElseGet(() -> {
                    context.getMonitor().warning("TargetNode file is not configured ('%s'). Federated Catalog Crawler will be inactive.".formatted(NODE_LIST_FILE));
                    return new NoopNodeDirectory();
                });

    }

    private static class NoopNodeDirectory implements TargetNodeDirectory {
        @Override
        public List<TargetNode> getAll() {
            return Collections.emptyList();
        }

        @Override
        public void insert(TargetNode targetNode) {

        }

        @Override
        public TargetNode remove(String s) {
            return null;
        }
    }
}
