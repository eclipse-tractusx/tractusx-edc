package org.eclipse.tractusx.edc.agreements.retirement.api.transform;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonObjectFromAgreementRetirementTransformer extends AbstractJsonLdTransformer<AgreementsRetirementEntry, JsonObject> {

    JsonBuilderFactory jsonFactory;

    protected JsonObjectFromAgreementRetirementTransformer(JsonBuilderFactory jsonFactory) {
        super(AgreementsRetirementEntry.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }


    @Override
    public @Nullable JsonObject transform(@NotNull AgreementsRetirementEntry entry, @NotNull TransformerContext transformerContext) {
        return jsonFactory.createObjectBuilder()
                .add(AgreementsRetirementEntry.AR_ENTRY_AGREEMENT_ID, entry.getAgreementId())
                .add(AgreementsRetirementEntry.AR_ENTRY_REASON, entry.getReason())
                .add(AgreementsRetirementEntry.AR_ENTRY_RETIREMENT_DATE, entry.getAgreementRetirementDate())
                .build();

    }
}
