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

/**
 * Statement templates and SQL table+column names required for the {@link SqlBusinessPartnerStore}
 */
public interface BusinessPartnerGroupStatements {

    default String getBpnColumn() {
        return "bpn";
    }

    default String getGroupsColumn() {
        return "groups";
    }

    default String getTable() {
        return "edc_business_partner_group";
    }

    String findByBpnTemplate();

    String insertTemplate();

    String deleteTemplate();

    String countQuery();

    String updateTemplate();
}
