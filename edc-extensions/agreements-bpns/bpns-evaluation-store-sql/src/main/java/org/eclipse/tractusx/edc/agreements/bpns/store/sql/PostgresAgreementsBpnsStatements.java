/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.agreements.bpns.store.sql;

import static java.lang.String.format;

public class PostgresAgreementsBpnsStatements implements SqlAgreementsBpnsStatements {

    @Override
    public String insertTemplate() {
        return executeStatement()
                .column(getAgreementIdColumn())
                .column(getProviderBpnColumn())
                .column(getConsumerBpnColumn())
                .insertInto(getTable());
    }

    @Override
    public String countQuery() {
        return format("SELECT COUNT (*) FROM %s WHERE %s = ?", getTable(), getAgreementIdColumn());
    }
}
