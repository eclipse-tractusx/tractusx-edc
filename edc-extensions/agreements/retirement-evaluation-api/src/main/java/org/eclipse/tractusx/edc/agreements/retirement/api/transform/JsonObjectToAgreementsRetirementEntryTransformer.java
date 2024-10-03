package org.eclipse.tractusx.edc.agreements.retirement.api.transform;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonObjectToAgreementsRetirementEntryTransformer extends AbstractJsonLdTransformer<JsonObject, AgreementsRetirementEntry> {

    public JsonObjectToAgreementsRetirementEntryTransformer() {
        super(JsonObject.class, AgreementsRetirementEntry.class);
    }

    @Override
    public @Nullable AgreementsRetirementEntry transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        return AgreementsRetirementEntry.Builder.newInstance()
                .withAgreementId(transformString(jsonObject.get(AgreementsRetirementEntry.AR_ENTRY_AGREEMENT_ID), context))
                .withAgreementId(transformString(jsonObject.get(AgreementsRetirementEntry.AR_ENTRY_AGREEMENT_ID), context))
                .withReason(transformString(jsonObject.get(AgreementsRetirementEntry.AR_ENTRY_REASON), context))
                .build();
    }
}
