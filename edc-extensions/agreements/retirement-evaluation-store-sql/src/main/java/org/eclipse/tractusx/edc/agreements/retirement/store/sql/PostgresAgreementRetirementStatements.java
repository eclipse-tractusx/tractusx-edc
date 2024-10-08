package org.eclipse.tractusx.edc.agreements.retirement.store.sql;

import org.eclipse.edc.sql.dialect.PostgresDialect;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;

public class PostgresAgreementRetirementStatements implements SqlAgreementsRetirementStatements {

    @Override
    public String findByIdTemplate() {
        return "";
    }

    @Override
    public String insertTemplate() {
        return format("INSERT INTO %s (%s, %s, %s) VALUES (?, ?%s)",
                getTable(), getIdColumn(), getReasonColumn(), getRetirementDateColumn(), getFormatJsonOperator());
    }

    @Override
    public String getDeleteByIdTemplate() {
        return format("DELETE FROM %s WHERE %s = ?", getTable(), getIdColumn());
    }

    @Override
    public String getCountByIdClause() {
        return format("SELECT COUNT (*) FROM %s WHERE %s = ?", getTable(), getIdColumn());
    }

    @Override
    public String getCountVariableName() {
        return "COUNT";
    }

    @NotNull
    private String getFormatJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }
}
