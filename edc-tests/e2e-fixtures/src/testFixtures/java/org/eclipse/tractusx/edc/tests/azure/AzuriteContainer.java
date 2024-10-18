/*
 * Copyright (c) 2024 Cofinity-X
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

package org.eclipse.tractusx.edc.tests.azure;

import org.testcontainers.containers.GenericContainer;

import java.util.List;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public class AzuriteContainer extends GenericContainer<AzuriteContainer> {

    private static final String IMAGE_NAME = "mcr.microsoft.com/azure-storage/azurite";
    private final int containerPort = 10_000;

    public AzuriteContainer(int azuriteHostPort, Account... accounts) {
        super(IMAGE_NAME);
        addEnv("AZURITE_ACCOUNTS", stream(accounts).map(it -> "%s:%s".formatted(it.name, it.key)).collect(joining(";")));
        setPortBindings(List.of("%d:%d".formatted(azuriteHostPort, containerPort)));
    }

    public AzureBlobHelper getHelper(Account account) {
        return new AzureBlobHelper(account.name(), account.key(), getHost(), getMappedPort(containerPort));
    }

    public record Account(String name, String key) { }
}
