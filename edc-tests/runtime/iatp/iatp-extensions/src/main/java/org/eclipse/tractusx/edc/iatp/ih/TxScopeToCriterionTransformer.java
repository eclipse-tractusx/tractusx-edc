/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.iatp.ih;

import org.eclipse.edc.identityhub.spi.transformation.ScopeToCriterionTransformer;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;

import java.util.List;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Implementation of {@link ScopeToCriterionTransformer} similar to the upstream one that maps tx scopes
 * to {@link Criterion} for querying the credentials (Just for testing)
 */
public class TxScopeToCriterionTransformer implements ScopeToCriterionTransformer {

    public static final String TYPE_OPERAND = "verifiableCredential.credential.type";
    public static final String ALIAS_LITERAL = "org.eclipse.tractusx.vc.type";
    public static final String CONTAINS_OPERATOR = "contains";
    private static final String SCOPE_SEPARATOR = ":";
    private final List<String> allowedOperations = List.of("read", "*", "all");

    @Override
    public Result<Criterion> transform(String scope) {
        var tokens = tokenize(scope);
        if (tokens.failed()) {
            return failure("Scope string cannot be converted: %s".formatted(tokens.getFailureDetail()));
        }
        var credentialType = tokens.getContent()[1];
        return success(new Criterion(TYPE_OPERAND, CONTAINS_OPERATOR, credentialType));
    }

    protected Result<String[]> tokenize(String scope) {
        if (scope == null) return failure("Scope was null");

        var tokens = scope.split(SCOPE_SEPARATOR);
        if (tokens.length != 3) {
            return failure("Scope string has invalid format.");
        }
        if (!ALIAS_LITERAL.equalsIgnoreCase(tokens[0])) {
            return failure("Scope alias MUST be %s but was %s".formatted(ALIAS_LITERAL, tokens[0]));
        }
        if (!allowedOperations.contains(tokens[2])) {
            return failure("Invalid scope operation: " + tokens[2]);
        }

        return success(tokens);
    }
}
