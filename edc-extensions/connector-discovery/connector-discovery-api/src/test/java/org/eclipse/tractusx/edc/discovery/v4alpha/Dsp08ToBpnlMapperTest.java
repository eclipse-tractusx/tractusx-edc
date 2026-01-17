/*
 * Copyright (c) 2026 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.discovery.v4alpha;

import org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants;
import org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants;
import org.eclipse.tractusx.edc.discovery.v4alpha.service.Dsp08ToBpnlMapper;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Dsp08ToBpnlMapperTest {
    private static final String TEST_DID = "did:web:example.com";
    private static final String TEST_BPN = "BPNL1234567890AB";
    private final BdrsClient bdrsClient = mock();

    final Dsp08ToBpnlMapper testee = new Dsp08ToBpnlMapper(bdrsClient);

    @ParameterizedTest
    @ArgumentsSource(Dsp08ToBpnlMapperTest.MappingProvider.class)
    void shouldReturnProperIdentifier_whenProperInputIsProvided(String dspVersion, String did, String expectation, boolean bdrsClientCall) {
        if (bdrsClientCall) {
            when(bdrsClient.resolveBpn(did)).thenReturn(expectation);
        }

        var mappedIdentifier = testee.identifierForDspVersion(did, dspVersion);

        assertThat(mappedIdentifier).isEqualTo(expectation);
        if (bdrsClientCall) {
            verify(bdrsClient, times(1)).resolveBpn(did);
        } else {
            verify(bdrsClient, never()).resolveBpn(anyString());
        }
    }

    private static class MappingProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    of(Dsp08Constants.V_08_VERSION, TEST_DID, TEST_BPN, true),
                    of(Dsp08Constants.V_08_VERSION, "DID:WEB:other.org", TEST_BPN, true),
                    of(Dsp2025Constants.V_2025_1_VERSION, TEST_DID, TEST_DID, false),
                    of(Dsp2025Constants.V_2025_1_VERSION, "DID:WEB:other.org", "DID:WEB:other.org", false)
            );
        }
    }

    @Test
    void shouldFail_whenWrongVersionRequested() {
        assertThatThrownBy(() -> testee.identifierForDspVersion(TEST_DID, "2024/1"))
                .hasMessageContaining("Given dsp version not supported");
        verify(bdrsClient, never()).resolveBpn(anyString());

    }

    @Test
    void shouldFail_whenWrongBpnUnknown() {
        when(bdrsClient.resolveBpn(TEST_DID)).thenReturn(null);
        assertThatThrownBy(() -> testee.identifierForDspVersion(TEST_DID, Dsp08Constants.V_08_VERSION))
                .hasMessageContaining("no BPNL found");
        verify(bdrsClient, times(1)).resolveBpn(TEST_DID);

    }
}
