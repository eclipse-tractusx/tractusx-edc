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