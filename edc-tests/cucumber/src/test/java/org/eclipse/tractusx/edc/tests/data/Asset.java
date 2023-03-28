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
package org.eclipse.tractusx.edc.tests.data;

import java.util.Objects;

public class Asset {
    private final String id;
    private final String description;
    private final DataAddress dataAddress;

    public Asset(String id, String description, DataAddress dataAddress) {
        this.id = Objects.requireNonNull(id);
        this.description = Objects.requireNonNull(description);
        this.dataAddress = Objects.requireNonNull(dataAddress);
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public DataAddress getDataAddress() {
        return dataAddress;
    }
}
