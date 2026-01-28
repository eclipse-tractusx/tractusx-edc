/********************************************************************************
 * Copyright (c) 2026 SAP SE
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

package org.eclipse.tractusx.edc.protocol.core.identifier;

import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.ServiceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultParticipantIdentityResolverTest {

    private static final String PARTICIPANT_CONTEXT_ID = "participant-context-id-123";
    private static final String PARTICIPANT_ID = "participant-id-123";

    private final ParticipantContextSupplier participantContextSupplier = mock(ParticipantContextSupplier.class);

    private DefaultParticipantIdentityResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DefaultParticipantIdentityResolver(participantContextSupplier);
    }

    @Test
    void getParticipantId_success() {

        var participantContext = ParticipantContext.Builder.newInstance()
                .identity(PARTICIPANT_ID).participantContextId(PARTICIPANT_CONTEXT_ID).build();
        when(participantContextSupplier.get()).thenReturn(ServiceResult.success(participantContext));

        var result = resolver.getParticipantId(PARTICIPANT_CONTEXT_ID, null);

        assertThat(result).isEqualTo(PARTICIPANT_ID);
    }

    @Test
    void getParticipantId_participantContextMissing() {

        when(participantContextSupplier.get()).thenReturn(ServiceResult.notFound("not found"));

        assertThatThrownBy(() -> resolver.getParticipantId(PARTICIPANT_CONTEXT_ID, null))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Cannot get the participant context: not found");
    }
}
