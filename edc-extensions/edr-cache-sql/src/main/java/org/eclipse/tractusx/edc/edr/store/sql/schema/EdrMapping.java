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

import org.eclipse.edc.sql.translation.TranslationMapping;
import org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceEntry;

/**
 * Maps fields of a {@link EndpointDataReferenceEntry} onto the
 * corresponding SQL schema (= column names)
 */
public class EdrMapping extends TranslationMapping {
    public EdrMapping(EdrStatements statements) {
        add("assetId", statements.getAssetIdColumn());
        add("agreementId", statements.getAgreementIdColumn());
    }
}
