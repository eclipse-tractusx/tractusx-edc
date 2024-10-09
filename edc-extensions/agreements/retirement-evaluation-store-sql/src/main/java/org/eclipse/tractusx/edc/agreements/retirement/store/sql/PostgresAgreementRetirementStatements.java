package org.eclipse.tractusx.edc.agreements.retirement.store.sql;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.dialect.PostgresDialect;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;

public class PostgresAgreementRetirementStatements implements SqlAgreementsRetirementStatements {

    private final SqlOperatorTranslator operatorTranslator;

    public PostgresAgreementRetirementStatements() {
        this.operatorTranslator = new PostgresqlOperatorTranslator();
    }

    @Override
    public String findByIdTemplate() {
        return format("SELECT * from %s WHERE %s = ?", getTable(), getIdColumn());
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

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(findByIdTemplate(), querySpec, new AgreementRetirementMapping(this), operatorTranslator);
    }

    @NotNull
    private String getFormatJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }
}
