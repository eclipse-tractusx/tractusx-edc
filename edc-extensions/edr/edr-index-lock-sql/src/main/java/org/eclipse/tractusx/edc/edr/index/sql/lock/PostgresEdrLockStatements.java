/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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

package org.eclipse.tractusx.edc.edr.index.sql.lock;


import org.eclipse.edc.edr.store.index.sql.schema.EndpointDataReferenceEntryStatements;
import org.eclipse.edc.edr.store.index.sql.schema.postgres.PostgresDialectStatements;

import static java.lang.String.format;


public class PostgresEdrLockStatements implements EdrLockStatements {

    private final EndpointDataReferenceEntryStatements edrEntryStatements;

    PostgresEdrLockStatements() {
        edrEntryStatements = new PostgresDialectStatements();
    }

    @Override
    public String getSelectForUpdateTemplate() {
        return format("SELECT * FROM %s WHERE %s = ? FOR UPDATE;", edrEntryStatements.getEdrEntryTable(), edrEntryStatements.getTransferProcessIdColumn());
    }

    @Override
    public String getCreatedAtColumn() {
        return edrEntryStatements.getCreatedAtColumn();
    }

    @Override
    public String getAssetIdColumn() {
        return edrEntryStatements.getAssetIdColumn();
    }

    @Override
    public String getTransferProcessIdColumn() {
        return edrEntryStatements.getTransferProcessIdColumn();
    }

    @Override
    public String getAgreementIdColumn() {
        return edrEntryStatements.getAgreementIdColumn();
    }

    @Override
    public String getProviderIdColumn() {
        return edrEntryStatements.getProviderIdColumn();
    }

    @Override
    public String getContractNegotiationIdColumn() {
        return edrEntryStatements.getContractNegotiationIdColumn();
    }

}
