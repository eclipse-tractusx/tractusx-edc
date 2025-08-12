/********************************************************************************
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.policy.cx.common;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.result.Result;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public abstract class ValueValidatingConstraintFunction<T extends Rule, C extends ParticipantAgentPolicyContext> extends BaseConstraintFunction<T, C> {
    private final Set<String> allowedValues;
    private final String pattern;
    private final boolean validateList;

    protected ValueValidatingConstraintFunction(Set<Operator> allowedOperators, String pattern) {
        this(allowedOperators, Set.of(), pattern, false);
    }

    protected ValueValidatingConstraintFunction(Set<Operator> allowedOperators, Set<String> validValues) {
        this(allowedOperators, validValues, "[\\s\\S]+", false);
    }

    protected ValueValidatingConstraintFunction(Set<Operator> allowedOperators, String pattern, boolean validateList) {
        this(allowedOperators, Set.of(), pattern, validateList);
    }

    protected ValueValidatingConstraintFunction(Set<Operator> allowedOperators, Set<String> validValues, boolean validateList) {
        this(allowedOperators, validValues, "[\\s\\S]+", validateList);
    }

    protected ValueValidatingConstraintFunction(Set<Operator> allowedOperators,
                                                Set<String> validValues,
                                                String pattern,
                                                boolean validateList) {
        super(allowedOperators);
        this.allowedValues = validValues;
        this.pattern = pattern;
        this.validateList = validateList;
    }

    @Override
    protected Result<Void> validateRightOperand(Object rightValue) {
        return validateList ? validateList(rightValue) : validateSingleValue(rightValue);
    }

    private Result<Void> validateSingleValue(Object rightValue) {
        if (allowedValues != null && !allowedValues.isEmpty()) {
            return rightValue instanceof String && allowedValues.contains(rightValue.toString())
                    ? Result.success()
                    : Result.failure("Invalid right-operand: this constraint only allows the following right-operands: %s, but received '%s'."
                    .formatted(String.join(", ", allowedValues), rightValue));
        }

        var compiledPattern = Pattern.compile(pattern);
        return rightValue instanceof String s && compiledPattern.matcher(s).matches()
                ? Result.success()
                : Result.failure("Invalid right-operand: right operand must match pattern '%s'".formatted(pattern));
    }

    private Result<Void> validateList(Object rightValue) {
        List<?> list = List.of();
        if (rightValue instanceof String s) {
            list = s.contains(",") ?
                    Arrays.stream(s.split(","))
                            .map(String::trim)
                            .toList() :
                    List.of(s.trim());
        }
        if (rightValue instanceof List<?> rightValuelist) {
            list = rightValuelist;
        }

        if (list.isEmpty()) {
            return Result.failure("Invalid right-operand: must be a list and contain at least 1 value");
        }

        if (allowedValues != null && !allowedValues.isEmpty()) {
            var invalidValues = list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(value -> !allowedValues.contains(value))
                    .toList();

            return invalidValues.isEmpty()
                    ? Result.success()
                    : Result.failure("Invalid right-operand: the following values are not allowed: %s".formatted(invalidValues));
        }

        var compiledPattern = Pattern.compile(pattern);
        var distinctValues = list.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(s -> compiledPattern.matcher(s).matches())
                .distinct()
                .count();

        return distinctValues == list.size()
                ? Result.success()
                : Result.failure("Invalid right-operand: list must contain only unique values matching pattern: %s".formatted(pattern));
    }
}
