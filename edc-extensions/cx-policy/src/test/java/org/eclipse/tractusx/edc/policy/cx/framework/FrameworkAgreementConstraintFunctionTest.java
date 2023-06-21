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

package org.eclipse.tractusx.edc.policy.cx.framework;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.json.JsonObject;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.eclipse.edc.policy.model.Operator.GEQ;
import static org.eclipse.edc.policy.model.Operator.GT;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.CX_USE_CASE_NS_V1;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.VP_PROPERTY;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdTextFixtures.createObjectMapper;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdTextFixtures.expand;
import static org.eclipse.tractusx.edc.policy.cx.framework.PcfCredential.PCF_VP;
import static org.eclipse.tractusx.edc.policy.cx.framework.UseCaseContext.USE_CASE_CONTEXT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FrameworkAgreementConstraintFunctionTest {
    private static final Map<String, String> CONTEXT_CACHE = Map.of(CX_USE_CASE_NS_V1, USE_CASE_CONTEXT);
    private Permission permission;
    private PolicyContext context;

    @Test
    void verify_constraint() throws JsonProcessingException {
        var function = FrameworkAgreementConstraintFunction.Builder
                .newInstance("PcfCredential")
                .agreementType("PcfAgreement")
                .build();

        setVpInContextVp();

        var result = function.evaluate(EQ, "active", permission, context);

        assertThat(result).isTrue();
    }

    @Test
    void verify_contract_version() throws JsonProcessingException {
        var function = FrameworkAgreementConstraintFunction.Builder
                .newInstance("PcfCredential")
                .agreementType("PcfAgreement")
                .agreementVersion("1.0.0")
                .build();

        setVpInContextVp();

        var result = function.evaluate(EQ, "active", permission, context);
        assertThat(result).isTrue();

        result = function.evaluate(GEQ, "active", permission, context);
        assertThat(result).isTrue();

        result = function.evaluate(GT, "active", permission, context);
        assertThat(result).isFalse(); // should fail because version is equal
    }

    @Test
    void verify_contract_version_gt_fail() throws JsonProcessingException {
        var function = FrameworkAgreementConstraintFunction.Builder
                .newInstance("PcfCredential")
                .agreementType("PcfAgreement")
                .agreementVersion("2.0.0")
                .build();

        setVpInContextVp();

        var result = function.evaluate(GT, "active", permission, context);
        assertThat(result).isFalse(); // should fail because version is equal

        verify(context, times(1)).reportProblem(Mockito.contains("version"));
    }

    @Test
    void verify_invalid_agreement_fail() throws JsonProcessingException {
        var function = FrameworkAgreementConstraintFunction.Builder
                .newInstance("PcfCredential")
                .agreementType("UnknownAgreement")
                .build();

        setVpInContextVp();

        var result = function.evaluate(EQ, "active", permission, context);

        assertThat(result).isFalse();

        verify(context, times(1)).reportProblem(Mockito.contains("missing the usecase type"));
    }

    @Test
    void verify_no_credential_fail() {
        var function = FrameworkAgreementConstraintFunction.Builder
                .newInstance("PcfCredential")
                .agreementType("PcfAgreement")
                .build();

        when(context.getParticipantAgent()).thenReturn(new ParticipantAgent(Map.of(), Map.of()));

        var result = function.evaluate(EQ, "active", permission, context);

        assertThat(result).isFalse();

        verify(context, times(1)).reportProblem(Mockito.contains("VP not found"));
    }

    @BeforeEach
    void setUp() {
        permission = Permission.Builder.newInstance().build();
        context = mock(PolicyContext.class);
    }

    private void setVpInContextVp() throws JsonProcessingException {
        var vp = expand(createObjectMapper().readValue(PCF_VP, JsonObject.class), CONTEXT_CACHE);
        when(context.getParticipantAgent()).thenReturn(new ParticipantAgent(Map.of(VP_PROPERTY, vp), Map.of()));
    }


}
