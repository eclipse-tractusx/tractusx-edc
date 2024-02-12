/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.edr.core.defaults;

import org.eclipse.edc.spi.query.PropertyLookup;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;

/**
 * This class is almost a 1:1 copy of the {@code CriterionToPredicateConverterImpl} (except for the {@code property()} method) from the {@code control-plane-core} module.
 * Pulling in that module is not possible, because that would pull in almost the entire Control Plane
 */
public class EdrCacheEntryPropertyLookup implements PropertyLookup {

    public static final String ASSET_ID = "assetId";
    public static final String AGREEMENT_ID = "agreementId";
    public static final String PROVIDER_ID = "providerId";
    public static final String CONTRACT_NEGOTIATION_ID = "contractNegotiationId";
    public static final String STATE = "state";

    @Override
    public Object getProperty(String key, Object object) {
        return property(key, object);
    }

    protected Object property(String key, Object object) {
        if (object instanceof EndpointDataReferenceEntry entry) {
            return switch (key) {
                case ASSET_ID -> entry.getAssetId();
                case AGREEMENT_ID -> entry.getAgreementId();
                case PROVIDER_ID -> entry.getProviderId();
                case CONTRACT_NEGOTIATION_ID -> entry.getContractNegotiationId();
                case STATE -> entry.getState();
                default -> null;
            };
        }
        return null;
    }

}
