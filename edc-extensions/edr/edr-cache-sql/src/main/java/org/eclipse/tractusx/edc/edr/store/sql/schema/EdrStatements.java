/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.edr.store.sql.schema;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.lease.LeaseStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Sql Statements for DataPlane Store
 */
public interface EdrStatements extends LeaseStatements {

    default String getEdrTable() {
        return "edc_edr_cache";
    }

    default String getTransferProcessIdColumn() {
        return "transfer_process_id";
    }

    default String getAgreementIdColumn() {
        return "agreement_id";
    }

    default String getProviderIdColumn() {
        return "provider_id";
    }

    default String getContractNegotiationIdColumn() {
        return "contract_negotiation_id";
    }

    default String getAssetIdColumn() {
        return "asset_id";
    }

    default String getEdrId() {
        return "edr_id";
    }

    default String getCreatedAtColumn() {
        return "created_at";
    }

    default String getUpdatedAtColumn() {
        return "updated_at";
    }

    default String getStateColumn() {
        return "state";
    }

    default String getExpirationTimestampColumn() {
        return "expiration_timestamp";
    }

    default String getStateCountColumn() {
        return "state_count";
    }

    default String getStateTimestampColumn() {
        return "state_timestamp";
    }

    default String getErrorDetailColumn() {
        return "error_detail";
    }

    String getFindByTransferProcessIdTemplate();

    SqlQueryStatement createQuery(QuerySpec querySpec);

    String getInsertTemplate();

    String getUpdateTemplate();

    String getDeleteByIdTemplate();

}

