/*
 *
 *   Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
 *
 *   See the NOTICE file(s) distributed with this work for additional
 *   information regarding copyright ownership.
 *
 *   This program and the accompanying materials are made available under the
 *   terms of the Apache License, Version 2.0 which is available at
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *   License for the specific language governing permissions and limitations
 *   under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.tractusx.edc.dataplane.tokenrefresh.e2e;

import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;


@EndToEndTest
public class DataPlaneTokenRefreshEndToEndTest {
    private static final int PROVIDER_CONTROL_PORT = getFreePort(); // port of the control api

    @RegisterExtension
    protected static final EdcRuntimeExtension DATAPLANE_RUNTIME = new EdcRuntimeExtension(
            ":edc-tests:runtime:dataplane-cloud",
            "Token-Refresh-Dataplane",
            RuntimeConfig.baseConfig("/signaling", PROVIDER_CONTROL_PORT)
    );

    @Test
    void foo() {
        // will be used once the RefreshAPI is here as well
    }
}
