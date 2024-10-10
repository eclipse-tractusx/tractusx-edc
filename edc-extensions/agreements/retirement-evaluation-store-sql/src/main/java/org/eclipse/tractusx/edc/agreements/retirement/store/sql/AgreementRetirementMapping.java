package org.eclipse.tractusx.edc.agreements.retirement.store.sql;

import org.eclipse.edc.sql.translation.TranslationMapping;

public class AgreementRetirementMapping extends TranslationMapping {
    private static final String FIELD_ID = "agreementId";
    private static final String FIELD_REASON = "reason";
    private static final String FIELD_AGREEMENT_RETIREMENT_DATE = "agreement_retirement_date";

    AgreementRetirementMapping(PostgresAgreementRetirementStatements statements) {
        add(FIELD_ID, statements.getIdColumn());
        add(FIELD_REASON, statements.getReasonColumn());
        add(FIELD_AGREEMENT_RETIREMENT_DATE, statements.getRetirementDateColumn());
    }
}
