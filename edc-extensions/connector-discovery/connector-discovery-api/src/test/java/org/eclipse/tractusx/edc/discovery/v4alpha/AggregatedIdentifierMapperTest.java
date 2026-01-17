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

import org.eclipse.tractusx.edc.discovery.v4alpha.service.AggregatedIdentifierMapper;
import org.eclipse.tractusx.edc.discovery.v4alpha.service.BpnMapper;
import org.eclipse.tractusx.edc.discovery.v4alpha.service.DidMapper;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.concurrent.ExecutionException;
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

public class AggregatedIdentifierMapperTest {
    private final BdrsClient bdrsClient = mock();

    private final DidMapper didTestee = new DidMapper();

    private final BpnMapper bpnTestee = new BpnMapper(bdrsClient);

    private final AggregatedIdentifierMapper testee =
            new AggregatedIdentifierMapper(didTestee, bpnTestee);

    @ParameterizedTest
    @ArgumentsSource(MappingProvider.class)
    void shouldReturnExpectedDid_whenProperInputIsProvided(String identifier, String expectation, boolean bdrsClientCall) throws ExecutionException, InterruptedException {
        if (bdrsClientCall) {
            when(bdrsClient.resolveDid(identifier)).thenReturn(expectation);
        }

        var mappedDid = testee.mapToDid(identifier);

        assertThat(mappedDid).isEqualTo(expectation);
        if (bdrsClientCall) {
            verify(bdrsClient, times(1)).resolveDid(identifier);
        } else {
            verify(bdrsClient, never()).resolveDid(anyString());
        }
    }

    private static class MappingProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    of("BPNL1234567890AB", "did:web:example.com", true),
                    of("did:web:example.com", "did:web:example.com", false),
                    of("bpnlabcdefghijkl", "did:web:other.org", true),
                    of("DID:WEB:other.org", "DID:WEB:other.org", false)
            );
        }
    }

    @ParameterizedTest
    @ArgumentsSource(CanHandleProvider.class)
    void shouldReturnCorrectEvaluation_whenCanHandleIsCalled(String identifier, boolean expected) {
        var result = testee.canHandle(identifier);

        assertThat(result).isEqualTo(expected);
    }

    private static class CanHandleProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    of("BPNL1234567890AB", true),
                    of("did:web:example.com", true),
                    of("bpnlabcdefghijkl", true),
                    of("DID:WEB:other.org", true),
                    of("https://example.com", false)
            );
        }
    }

    @Test
    void shouldFail_whenProvidedIdentifierIsNeitherBpnlNorDid() {
        assertThatThrownBy(() -> testee.mapToDid("https://example.com"))
                .hasMessageContaining("is of unknown type");
        verify(bdrsClient, never()).resolveDid(anyString());
    }

    @Test
    void shouldFail_whenProvidedBpnlCannotBeFound() {
        var testBpnl = "BPNL1234567890AB";
        when(bdrsClient.resolveDid(testBpnl)).thenReturn(null);
        assertThatThrownBy(() -> testee.mapToDid(testBpnl))
                .hasMessageContaining("not found as registered identity");
        verify(bdrsClient, times(1)).resolveDid(testBpnl);
    }

    @Test
    void shouldFail_whenDidMapperIsCalledWithWrongIdentifier() {
        assertThatThrownBy(() -> didTestee.mapToDid("BPNL1234567890AB"))
                .hasMessageContaining("is not a DID");
        verify(bdrsClient, never()).resolveDid(anyString());
    }

    @Test
    void shouldFail_whenBpnMapperIsCalledWithWrongIdentifier() {
        assertThatThrownBy(() -> bpnTestee.mapToDid("did:web:example.com"))
                .hasMessageContaining("is not a BPNL");
        verify(bdrsClient, never()).resolveDid(anyString());
    }
}
