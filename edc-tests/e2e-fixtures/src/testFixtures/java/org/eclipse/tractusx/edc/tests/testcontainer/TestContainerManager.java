/********************************************************************************
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.tests.testcontainer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;

public class TestContainerManager {
    public static String getContainerNameFromDependabotManagedDockerfile(Class<?> resourceProvider, String resourceName) {
        try (InputStream resourceInput = Optional.ofNullable(resourceProvider.getResourceAsStream(resourceName)).orElseThrow();
                BufferedReader reader = new BufferedReader(new InputStreamReader(resourceInput))) {
            return reader.lines()
                    .findFirst().orElseThrow()
                    .substring(5);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getContainerNameFromDependabotManagedDockerfile(Class<?> resourceProvider) {
        return getContainerNameFromDependabotManagedDockerfile(resourceProvider, "/Dockerfile");
    }
}
