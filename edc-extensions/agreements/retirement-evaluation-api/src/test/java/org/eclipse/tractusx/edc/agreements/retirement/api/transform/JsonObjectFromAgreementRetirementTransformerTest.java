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

package org.eclipse.tractusx.edc.agreements.retirement.api.transform;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_AGREEMENT_ID;
import static org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_REASON;
import static org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_RETIREMENT_DATE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JsonObjectFromAgreementRetirementTransformerTest {

    private final JsonBuilderFactory factory = Json.createBuilderFactory(Map.of());

    private final JsonObjectFromAgreementRetirementTransformer transformer = new JsonObjectFromAgreementRetirementTransformer(factory);

    @Test
    void transform() {

        var context = mock(TransformerContext.class);

        var entry = AgreementsRetirementEntry.Builder.newInstance()
                .withAgreementId("agreementId")
                .withReason("long-reason")
                .build();

        var result = transformer.transform(entry, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(AR_ENTRY_AGREEMENT_ID)).isEqualTo("agreementId");
        assertThat(result.getString(AR_ENTRY_REASON)).isEqualTo("long-reason");
        assertThat(result.getJsonNumber(AR_ENTRY_RETIREMENT_DATE)).isNotNull();
        verify(context, never()).reportProblem(anyString());
    }

}