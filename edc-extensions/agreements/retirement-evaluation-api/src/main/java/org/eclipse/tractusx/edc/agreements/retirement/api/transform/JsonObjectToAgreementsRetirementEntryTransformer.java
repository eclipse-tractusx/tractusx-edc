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
    public @Nullable AgreementsRetirementEntry transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext transformerContext) {
        return AgreementsRetirementEntry.Builder.newInstance()
                .withAgreementId(jsonObject.getString(AgreementsRetirementEntry.AR_ENTRY_AGREEMENT_ID))
                .withReason(jsonObject.getString(AgreementsRetirementEntry.AR_ENTRY_REASON))
                .build();
    }
}
