/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.tests.transfer.iatp.runtime;

import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;

import java.security.KeyPair;
import java.util.Map;

public interface Runtimes {

    static RuntimeExtension dimRuntime(String name, Map<String, String> properties, KeyPair keyPair) {
        return new IatpParticipantRuntimeExtension(":edc-tests:runtime:iatp:runtime-memory-iatp-dim-ih",
                name,
                properties,
                keyPair);
    }

    static RuntimeExtension iatpRuntime(String name, Map<String, String> properties, KeyPair keyPair) {
        return new IatpParticipantRuntimeExtension(":edc-tests:runtime:iatp:runtime-memory-iatp-ih",
                name,
                properties,
                keyPair);
    }

    static RuntimeExtension stsRuntime(String name, Map<String, String> properties, KeyPair keyPair) {
        return new IatpParticipantRuntimeExtension(
                ":edc-tests:runtime:iatp:runtime-memory-sts",
                name,
                properties,
                keyPair);
    }

}
