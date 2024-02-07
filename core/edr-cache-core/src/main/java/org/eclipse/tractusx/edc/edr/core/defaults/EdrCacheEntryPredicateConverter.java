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

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.CriterionToPredicateConverter;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * This class is almost a 1:1 copy of the {@code CriterionToPredicateConverterImpl} (except for the {@code property()} method) from the {@code control-plane-core} module.
 * Pulling in that module is not possible, because that would pull in almost the entire Control Plane
 */
public class EdrCacheEntryPredicateConverter implements CriterionToPredicateConverter {

    public static final String ASSET_ID = "assetId";
    public static final String AGREEMENT_ID = "agreementId";
    public static final String PROVIDER_ID = "providerId";
    public static final String CONTRACT_NEGOTIATION_ID = "contractNegotiationId";
    public static final String STATE = "state";

    @Override
    public <T> Predicate<T> convert(Criterion criterion) {
        var operator = criterion.getOperator().toLowerCase();

        return switch (operator) {
            case "=" -> equalPredicate(criterion);
            case "in" -> inPredicate(criterion);
            case "like" -> likePredicate(criterion);
            default ->
                    throw new IllegalArgumentException(format("Operator [%s] is not supported by this converter!", criterion.getOperator()));
        };
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
        throw new IllegalArgumentException("Can only handle objects of type " + EndpointDataReferenceEntry.class.getSimpleName() + " but received an " + object.getClass().getSimpleName());
    }

    @NotNull
    private <T> Predicate<T> equalPredicate(Criterion criterion) {
        return t -> {
            var operandLeft = (String) criterion.getOperandLeft();
            var property = property(operandLeft, t);
            if (property == null) {
                return false;
            }

            if (property.getClass().isEnum() && criterion.getOperandRight() instanceof String) {
                var enumProperty = (Enum<?>) property;
                return Objects.equals(enumProperty.name(), criterion.getOperandRight());
            }

            if (property instanceof Number c1 && criterion.getOperandRight() instanceof Number c2) {
                // interpret as double to not lose any precision
                return Double.compare(c1.doubleValue(), c2.doubleValue()) == 0;
            }

            if (property instanceof List<?> list) {
                return list.stream().anyMatch(it -> Objects.equals(it, criterion.getOperandRight()));
            }

            return Objects.equals(property, criterion.getOperandRight());
        };
    }

    @NotNull
    private <T> Predicate<T> inPredicate(Criterion criterion) {
        return t -> {
            var operandLeft = (String) criterion.getOperandLeft();
            var property = property(operandLeft, t);
            if (property == null) {
                return false;
            }

            if (criterion.getOperandRight() instanceof Iterable<?> iterable) {
                for (var value : iterable) {
                    if (value.equals(property)) {
                        return true;
                    }
                }
                return false;
            } else {
                throw new IllegalArgumentException("Operator IN requires the right-hand operand to be an " + Iterable.class.getName() + " but was " + criterion.getOperandRight().getClass().getName());
            }


        };
    }

    @NotNull
    private <T> Predicate<T> likePredicate(Criterion criterion) {
        return t -> {
            var operandLeft = (String) criterion.getOperandLeft();
            var property = property(operandLeft, t);
            if (property == null) {
                return false;
            }

            if (criterion.getOperandRight() instanceof String operandRight) {
                var regexPattern = Pattern.quote(operandRight)
                        .replace("%", "\\E.*\\Q")
                        .replace("_", "\\E.\\Q");

                regexPattern = "^" + regexPattern + "$";

                return Pattern.compile(regexPattern).matcher(property.toString()).matches();
            }

            return false;
        };
    }
}
