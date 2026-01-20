/*******************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2025 Cofinity-X GmbH
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
 ******************************************************************************/

package org.eclipse.tractusx.edc.compatibility.tests.fixtures;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.Map;

public enum EdcDockerRuntimes {

    CONTROL_PLANE("controlplane-stable:latest"),
    DATA_PLANE("dataplane-stable:latest");

    private final String image;

    EdcDockerRuntimes(String image) {
        this.image = image;
    }

    public GenericContainer<?> create(String name, Map<String, String> env) {
        return new GenericContainer<>(image)
                .withCreateContainerCmdModifier(cmd -> cmd.withName(name))
                .withNetworkMode("host")
                .withLogConsumer(it -> System.out.println("[%s] %s".formatted(name, it.getUtf8StringWithoutLineEnding())))
                .waitingFor(Wait.forLogMessage(".*Runtime .* ready.*", 1))
                .withEnv(env);
    }

}
