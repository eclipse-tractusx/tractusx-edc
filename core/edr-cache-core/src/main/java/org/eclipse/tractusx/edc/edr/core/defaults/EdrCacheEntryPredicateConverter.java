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

import org.eclipse.edc.connector.defaults.storage.CriterionToPredicateConverterImpl;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;

public class EdrCacheEntryPredicateConverter extends CriterionToPredicateConverterImpl {

    @Override
    protected Object property(String key, Object object) {
        if (object instanceof EndpointDataReferenceEntry entry) {
            return switch (key) {
                case "assetId" -> entry.getAssetId();
                case "agreementId" -> entry.getAgreementId();
                case "providerId" -> entry.getProviderId();
                case "state" -> entry.getState();
                default -> null;
            };
        }
        throw new IllegalArgumentException("Can only handle objects of type " + EndpointDataReferenceEntry.class.getSimpleName() + " but received an " + object.getClass().getSimpleName());
    }
}
