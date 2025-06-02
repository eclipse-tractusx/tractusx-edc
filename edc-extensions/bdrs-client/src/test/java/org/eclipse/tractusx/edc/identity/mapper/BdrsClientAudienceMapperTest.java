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

package org.eclipse.tractusx.edc.identity.mapper;

import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BdrsClientAudienceMapperTest {

    private final BdrsClient bdrsClient = mock();
    private final BdrsClientAudienceMapper clientAudienceMapper = new BdrsClientAudienceMapper(bdrsClient);

    @Test
    void shouldReturnDid() {
        when(bdrsClient.resolve("bpn1")).thenReturn("did:web:did1");

        var did = clientAudienceMapper.resolve(new TestMessage("bpn1"));

        assertThat(did).isSucceeded().isEqualTo("did:web:did1");
    }

    @Test
    void shouldFail_whenResolutionFails() {
        when(bdrsClient.resolve("bpn1")).thenReturn(null);

        var did = clientAudienceMapper.resolve(new TestMessage("bpn1"));

        assertThat(did).isFailed();
    }

    @Test
    void shouldFail_whenResolutionThrowsException() {
        when(bdrsClient.resolve("bpn1")).thenThrow(new RuntimeException("exception"));

        var did = clientAudienceMapper.resolve(new TestMessage("bpn1"));

        assertThat(did).isFailed().detail().contains("exception");
    }

    private record TestMessage(String bpn) implements RemoteMessage {
        @Override
        public String getProtocol() {
            return "test-proto";
        }

        @Override
        public String getCounterPartyAddress() {
            return "http://bpn1";
        }

        @Override
        public String getCounterPartyId() {
            return bpn;
        }
    }
}
