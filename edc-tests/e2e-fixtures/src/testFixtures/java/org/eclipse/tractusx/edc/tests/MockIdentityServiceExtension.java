/********************************************************************************
 * Copyright (c) 2026 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.tests;

import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.system.ServiceExtension;

public class MockIdentityServiceExtension implements ServiceExtension {
    @Inject
    private IdentityService identityService;    //ensure the original IdentityService dependencies are injected before overriding with the mocked class

    @Inject
    private JtiValidationStore jtiValidationStore;

    @Inject
    private DidPublicKeyResolver didPublicKeyResolver;

    private final String bpn;
    private final String did;

    public MockIdentityServiceExtension(String bpn, String did) {
        this.bpn = bpn;
        this.did = did;
    }

    @Provider
    public IdentityService mockIdentityService() {
        var mockTokenValidationService = new MockTokenValidationService();
        var mockTokenValidationAction = new MockTokenValidationAction(mockTokenValidationService, didPublicKeyResolver, jtiValidationStore);
        return new MockVcIdentityService(bpn, did, mockTokenValidationAction);
    }
}
