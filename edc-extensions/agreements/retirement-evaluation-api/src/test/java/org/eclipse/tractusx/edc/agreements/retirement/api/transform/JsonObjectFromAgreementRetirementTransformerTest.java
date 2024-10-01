package org.eclipse.tractusx.edc.agreements.retirement.api.transform;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JsonObjectFromAgreementRetirementTransformerTest {

    JsonBuilderFactory factory = Json.createBuilderFactory(Map.of());

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
        assertThat(result.getString(AgreementsRetirementEntry.AR_ENTRY_AGREEMENT_ID)).isEqualTo("agreementId");
        assertThat(result.getString(AgreementsRetirementEntry.AR_ENTRY_REASON)).isEqualTo("long-reason");
        assertThat(result.getString(AgreementsRetirementEntry.AR_ENTRY_RETIREMENT_DATE)).isNotNull();
        verify(context, never()).reportProblem(anyString());
    }

}