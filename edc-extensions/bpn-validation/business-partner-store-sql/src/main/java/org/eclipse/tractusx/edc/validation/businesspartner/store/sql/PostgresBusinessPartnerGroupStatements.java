/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.validation.businesspartner.store.sql;

import org.eclipse.edc.sql.dialect.PostgresDialect;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;

/**
 * Postgres-specific statement templates for the {@link SqlBusinessPartnerStore}
 */
public class PostgresBusinessPartnerGroupStatements implements BusinessPartnerGroupStatements {
    @Override
    public String findByBpnTemplate() {
        return format("SELECT %s from %s WHERE %s = ?", getGroupsColumn(), getTable(), getBpnColumn());
    }

    @Override
    public String insertTemplate() {
        return format("INSERT INTO %s (%s, %s) VALUES (?, ?%s)", getTable(), getBpnColumn(), getGroupsColumn(), getFormatJsonOperator());
    }

    @Override
    public String deleteTemplate() {
        return format("DELETE FROM %s WHERE %s = ?", getTable(), getBpnColumn());
    }

    @Override
    public String countQuery() {
        return format("SELECT COUNT (*) FROM %s WHERE %s = ?", getTable(), getBpnColumn());
    }

    @Override
    public String updateTemplate() {
        return format("UPDATE %s SET %s=?%s WHERE %s=?", getTable(), getGroupsColumn(), getFormatJsonOperator(), getBpnColumn());
    }

    @NotNull
    private String getFormatJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }
}
