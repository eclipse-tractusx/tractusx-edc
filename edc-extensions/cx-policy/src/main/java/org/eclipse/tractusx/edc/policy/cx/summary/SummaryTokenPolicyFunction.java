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

package org.eclipse.tractusx.edc.policy.cx.summary;

import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.TokenParameters;

import java.util.function.BiFunction;

import static java.lang.String.format;

/**
 * Includes a summary credential in the token parameters.
 */
public class SummaryTokenPolicyFunction implements  BiFunction<Policy, PolicyContext, Boolean> {

    @Override
    public Boolean apply(Policy policy, PolicyContext context) {
        var params = context.getContextData(TokenParameters.Builder.class);
        if (params == null) {
            throw new EdcException(format("%s not set in policy context", TokenParameters.Builder.class.getName()));
        }
        // TODO set summary credential when we upgrade to the latest EDC snapshot
        return true;
    }
}
