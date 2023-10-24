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
                case "assetId" -> entry.getAssetId();
                case "agreementId" -> entry.getAgreementId();
                case "providerId" -> entry.getProviderId();
                case "contractNegotiationId" -> entry.getContractNegotiationId();
                case "state" -> entry.getState();
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
