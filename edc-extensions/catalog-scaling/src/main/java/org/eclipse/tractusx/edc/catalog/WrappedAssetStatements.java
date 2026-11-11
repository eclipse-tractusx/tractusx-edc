/*
 * Copyright (c) 2026 Volkswagen AG
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
 */

package org.eclipse.tractusx.edc.catalog;

import org.eclipse.edc.connector.controlplane.store.sql.assetindex.schema.postgres.PostgresDialectStatements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Extends the Postgres dialect for
 * {@link org.eclipse.edc.connector.controlplane.store.sql.assetindex.schema.AssetStatements}, which
 * are used for generating any query targeting the assets table. Overrides the select statement
 * and replaces the table name with a call to the Postgres function for finding assets matching
 * authorized contract definitions.
 * <p>
 * Does not have any impact on other asset queries, as this instance is not supplied to the
 * extension context and is only used within this module.
 */
public class WrappedAssetStatements extends PostgresDialectStatements {

    /**
     * Queries for assets using the Postgres function 'edc_find_matching_assets_page' instead of
     * the assets table.
     *
     * @return the select statement targeting the 'edc_find_matching_assets_page' function
     */
    @Override
    public String getSelectAssetTemplate() {
        return "SELECT * FROM edc_find_matching_assets_page(?) AS a";
    }

    /**
     * Prepends the temporary contract definition table name to the parameters of a statement created
     * from {@link #getSelectAssetTemplate()}.
     * <p>
     * The placeholder for the table name sits in the select clause, which
     * {@code SqlQueryStatement#getQueryAsString()} always renders before the where clause and the
     * trailing limit/offset. The table name must therefore be the first parameter.
     *
     * @param tempTableName the name of the temporary contract definition table.
     * @param parameters the parameters of the generated query, in order.
     * @return the parameters prefixed with the table name.
     */
    public static Object[] withTempTableName(String tempTableName, Object... parameters) {
        List<Object> result = new ArrayList<>();
        result.add(tempTableName);
        result.addAll(Arrays.asList(parameters));
        return result.toArray();
    }
}
