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

package org.eclipse.tractusx.edc.edr.core.defaults;

import org.eclipse.edc.spi.query.BaseCriterionToPredicateConverter;
import org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceEntry;

public class EdrCacheEntryPredicateConverter extends BaseCriterionToPredicateConverter<EndpointDataReferenceEntry> {

    @Override
    protected Object property(String key, Object object) {
        if (object instanceof EndpointDataReferenceEntry) {
            var entry = (EndpointDataReferenceEntry) object;
            switch (key) {
                case "assetId":
                    return entry.getAssetId();
                case "agreementId":
                    return entry.getAgreementId();
                default:
                    return null;
            }
        }
        throw new IllegalArgumentException("Can only handle objects of type " + EndpointDataReferenceEntry.class.getSimpleName() + " but received an " + object.getClass().getSimpleName());
    }
}
