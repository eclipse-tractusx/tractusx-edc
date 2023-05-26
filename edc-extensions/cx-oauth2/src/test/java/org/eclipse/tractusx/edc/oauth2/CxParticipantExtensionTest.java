/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.oauth2;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.agent.ParticipantAgent.PARTICIPANT_IDENTITY;
import static org.eclipse.tractusx.edc.oauth2.CxParticipantExtension.REFERRING_CONNECTOR_CLAIM;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
public class CxParticipantExtensionTest {

    CxParticipantExtension extension;

    ParticipantAgentService agentService = mock(ParticipantAgentService.class);

    ServiceExtensionContext context;

    @BeforeEach
    void setUp(ObjectFactory factory, ServiceExtensionContext context) {
        this.context = spy(context);
        context.registerService(ParticipantAgentService.class, agentService);
        extension = factory.constructInstance(CxParticipantExtension.class);
    }

    @Test
    void initialize() {
        extension.initialize(context);
        var attributes = Map.of(PARTICIPANT_IDENTITY, "BPNSOKRATES");
        verify(agentService).register(isA(CxParticipantExtension.class));
        var claims = ClaimToken.Builder.newInstance().claim(REFERRING_CONNECTOR_CLAIM, "http://sokrates-controlplane/BPNSOKRATES").build();


        assertThat(extension.attributesFor(claims)).containsExactlyEntriesOf(attributes);

        claims = ClaimToken.Builder.newInstance().claim(REFERRING_CONNECTOR_CLAIM, "http://sokrates-controlplane/BPNSOKRATES/").build();
        assertThat(extension.attributesFor(claims)).containsExactlyEntriesOf(attributes);

        claims = ClaimToken.Builder.newInstance().claim(REFERRING_CONNECTOR_CLAIM, "http://sokrates-controlplane/test/path/BPNSOKRATES/").build();
        assertThat(extension.attributesFor(claims)).containsExactlyEntriesOf(attributes);
    }


    @ParameterizedTest
    @ArgumentsSource(ClaimProvider.class)
    void attributesFor_shouldMatchTheId(Map<String, Object> claims) {
        var attributes = Map.of(PARTICIPANT_IDENTITY, "BPNSOKRATES");
        extension.initialize(context);
        var claimToken = ClaimToken.Builder.newInstance().claims(claims).build();
        assertThat(extension.attributesFor(claimToken)).containsExactlyEntriesOf(attributes);
    }

    static class ClaimProvider implements ArgumentsProvider {
        ClaimProvider() {
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Map.of(REFERRING_CONNECTOR_CLAIM, "http://sokrates-controlplane/BPNSOKRATES"),
                    Map.of(REFERRING_CONNECTOR_CLAIM, "http://sokrates-controlplane/BPNSOKRATES/"),
                    Map.of(REFERRING_CONNECTOR_CLAIM, "http://sokrates-controlplane/test/path/BPNSOKRATES"),
                    Map.of(REFERRING_CONNECTOR_CLAIM, "https://sokrates-controlplane/test/path/BPNSOKRATES"),
                    Map.of(REFERRING_CONNECTOR_CLAIM, "BPNSOKRATES")
            ).map(Arguments::arguments);
        }
    }
}

