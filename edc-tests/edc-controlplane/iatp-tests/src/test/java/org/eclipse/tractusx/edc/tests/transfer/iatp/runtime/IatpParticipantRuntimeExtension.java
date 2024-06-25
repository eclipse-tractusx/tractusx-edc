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

package org.eclipse.tractusx.edc.tests.transfer.iatp.runtime;

import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.security.KeyPair;
import java.util.Map;

public class IatpParticipantRuntimeExtension extends RuntimePerClassExtension implements AfterEachCallback {

    public IatpParticipantRuntimeExtension(String modulename, String name, Map<String, String> properties, KeyPair keyPair) {
        super(new IatpParticipantRuntime(modulename, name, properties, keyPair));
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        ((IatpParticipantRuntime) runtime).getWiper().clearPersistence();
    }
}
