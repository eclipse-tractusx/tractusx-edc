/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.flow;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.AUDIENCE_PROPERTY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TxDataFlowPropertiesProviderTest {

    private final BdrsClient bdrsClient = mock();
    private final TxDataFlowPropertiesProvider provider = new TxDataFlowPropertiesProvider(bdrsClient);

    @Test
    void shouldReturnProperties() {
        var bpn = "bpn";
        var did = "did";
        when(bdrsClient.resolve(bpn)).thenReturn(did);

        var result = provider.propertiesFor(createTransferProcess(), createPolicy(bpn));

        assertThat(result).isSucceeded().satisfies(properties -> {
            assertThat(properties).containsEntry(AUDIENCE_PROPERTY, did);
        });
    }

    @Test
    void shouldReturnFailure_whenResolutionFails() {
        var bpn = "bpn";
        when(bdrsClient.resolve(bpn)).thenReturn(null);

        var result = provider.propertiesFor(createTransferProcess(), createPolicy(bpn));

        assertThat(result).isFailed().detail().isEqualTo("Failed to fetch did for BPN %s".formatted(bpn));
    }

    @Test
    void shouldReturnFailure_whenResolutionThrowsException() {
        var bpn = "bpn";
        when(bdrsClient.resolve(bpn)).thenThrow(new RuntimeException("exception"));

        var result = provider.propertiesFor(createTransferProcess(), createPolicy(bpn));

        assertThat(result).isFailed().detail().contains("exception");
    }

    private TransferProcess createTransferProcess() {
        return TransferProcess.Builder.newInstance().build();
    }

    private Policy createPolicy(String bpn) {
        return Policy.Builder.newInstance().assignee(bpn).build();
    }
}
