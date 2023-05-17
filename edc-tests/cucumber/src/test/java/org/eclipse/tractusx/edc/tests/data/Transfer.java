/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.edc.tests.DataManagementAPI;
import org.eclipse.tractusx.edc.tests.util.Timeouts;

import java.io.IOException;
import java.time.Duration;

import static org.awaitility.Awaitility.await;


public class Transfer {

    private final String id;

    public Transfer(String id) {
        this.id = id;
    }

    public void waitUntilComplete(DataManagementAPI dataManagementAPI) {
        await()
                .pollDelay(Duration.ofMillis(2000))
                .atMost(Timeouts.FILE_TRANSFER)
                .until(() -> isComplete(dataManagementAPI));
    }

    public boolean isComplete(DataManagementAPI dataManagementAPI) throws IOException {
        var transferProcess = dataManagementAPI.getTransferProcess(id);
        if (transferProcess == null) {
            return false;
        }

        var state = transferProcess.getState();

        return state == TransferProcessState.COMPLETED || state == TransferProcessState.ERROR;
    }

    public String getId() {
        return id;
    }
}
