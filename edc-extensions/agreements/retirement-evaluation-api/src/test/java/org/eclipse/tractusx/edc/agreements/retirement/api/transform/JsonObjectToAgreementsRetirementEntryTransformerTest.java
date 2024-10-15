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
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

class JsonObjectToAgreementsRetirementEntryTransformerTest {

    private final JsonObjectToAgreementsRetirementEntryTransformer transformer = new JsonObjectToAgreementsRetirementEntryTransformer();

    @Test
    void transform() {

        var context = mock(TransformerContext.class);
        var jsonEntry = Json.createObjectBuilder()
                .add(AgreementsRetirementEntry.AR_ENTRY_AGREEMENT_ID, "agreementId")
                .add(AgreementsRetirementEntry.AR_ENTRY_REASON, "reason")
                .build();

        var result = transformer.transform(jsonEntry, context);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(AgreementsRetirementEntry.class);
        assertThat(result.getAgreementId()).isEqualTo("agreementId");
        assertThat(result.getReason()).isEqualTo("reason");
        assertThat(result.getAgreementRetirementDate()).isNotNull();
    }

}