/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
    public String findByBpnGroupTemplate() {
        return format("SELECT %s from %s WHERE EXISTS (SELECT 1 FROM json_array_elements_text(%s) AS group_element WHERE group_element = ?)",
                getBpnColumn(), getTable(), getGroupsColumn());
    }

    @Override
    public String findByBpnGroupsTemplate() {
        return format("SELECT DISTINCT group_element AS group_name from %s, json_array_elements_text(%s) AS group_element;",
                getTable(), getGroupsColumn());
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
