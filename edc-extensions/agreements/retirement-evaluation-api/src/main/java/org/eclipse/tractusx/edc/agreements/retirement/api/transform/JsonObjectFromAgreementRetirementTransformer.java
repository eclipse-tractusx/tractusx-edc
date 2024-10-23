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

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_AGREEMENT_ID;
import static org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_REASON;
import static org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_RETIREMENT_DATE;

public class JsonObjectFromAgreementRetirementTransformer extends AbstractJsonLdTransformer<AgreementsRetirementEntry, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromAgreementRetirementTransformer(JsonBuilderFactory jsonFactory) {
        super(AgreementsRetirementEntry.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }


    @Override
    public @Nullable JsonObject transform(@NotNull AgreementsRetirementEntry entry, @NotNull TransformerContext transformerContext) {
        return jsonFactory.createObjectBuilder()
                .add(AR_ENTRY_AGREEMENT_ID, entry.getAgreementId())
                .add(AR_ENTRY_REASON, entry.getReason())
                .add(AR_ENTRY_RETIREMENT_DATE, entry.getAgreementRetirementDate())
                .build();

    }
}
