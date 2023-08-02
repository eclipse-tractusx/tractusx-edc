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

package org.eclipse.tractusx.edc.validation.businesspartner.spi;

import org.eclipse.edc.spi.result.StoreResult;

import java.util.List;

public interface BusinessPartnerGroupStore {
    StoreResult<List<String>> resolveForBpn(String businessPartnerNumber);

    StoreResult<Void> save(String businessPartnerNumber, List<String> groups);

    StoreResult<Void> delete(String businessPartnerNumber);

    StoreResult<Void> update(String businessPartnerNumber, List<String> groups);
}
