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

package org.eclipse.tractusx.edc.tests;

import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

import java.util.Objects;
import java.util.function.Function;

/**
 * Mock implementation of {@link BdrsClient} backed by two lambdas.
 * The lambdas decide how DIDs and BPNs are resolved.
 */
public final class MockBdrsClient implements BdrsClient {
    private final Function<String, String> didResolver; // input: BPN -> output: DID
    private final Function<String, String> bpnResolver; // input: DID -> output: BPN

    public MockBdrsClient(Function<String, String> didResolver,
                          Function<String, String> bpnResolver) {
        this.didResolver = Objects.requireNonNull(didResolver, "didResolver");
        this.bpnResolver = Objects.requireNonNull(bpnResolver, "bpnResolver");
    }

    @Override
    public String resolveDid(String bpn) {
        return didResolver.apply(bpn);
    }

    @Override
    public String resolveBpn(String did) {
        return bpnResolver.apply(did);
    }
}
