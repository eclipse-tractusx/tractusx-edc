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

package org.eclipse.tractusx.edc.policy.cx.summary;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.json.JsonObject;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.SummaryContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.CX_SUMMARY_NS_V1;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.VP_PROPERTY;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdTextFixtures.createObjectMapper;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdTextFixtures.expand;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.SummaryCredential.SUMMARY_VP;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SummaryConstraintFunctionTest {
    public static final String CX_QUALITY = "QualityCredential";
    private static final Map<String, String> CONTEXT_CACHE = Map.of(CX_SUMMARY_NS_V1, SummaryContext.SUMMARY_CONTEXT);
    private Permission permission;
    private PolicyContext context;

    @Test
    void verify_constraint_success() throws JsonProcessingException {
        var vp = expand(createObjectMapper().readValue(SUMMARY_VP, JsonObject.class), CONTEXT_CACHE);

        var function = new SummaryConstraintFunction(CX_QUALITY);

        when(context.getContextData(ParticipantAgent.class)).thenReturn(new ParticipantAgent(Map.of(VP_PROPERTY, vp), Map.of()));

        var result = function.evaluate(EQ, "active", permission, context);

        assertThat(result).isTrue();

        verify(context, atLeastOnce()).getContextData(ParticipantAgent.class);
    }

    @BeforeEach
    void setUp() {
        permission = Permission.Builder.newInstance().build();
        context = mock(PolicyContext.class);
    }
}
