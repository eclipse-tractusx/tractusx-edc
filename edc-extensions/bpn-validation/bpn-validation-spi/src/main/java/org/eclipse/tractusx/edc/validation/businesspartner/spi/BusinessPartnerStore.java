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

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.List;

@ExtensionPoint
public interface BusinessPartnerStore {
    String NOT_FOUND_TEMPLATE = "BPN %s was not found";
    String ALREADY_EXISTS_TEMPLATE = "BPN %s already exists in database";

    StoreResult<List<String>> resolveForBpn(String businessPartnerNumber);

    StoreResult<Void> save(String businessPartnerNumber, List<String> groups);

    StoreResult<Void> delete(String businessPartnerNumber);

    StoreResult<Void> update(String businessPartnerNumber, List<String> groups);
}
