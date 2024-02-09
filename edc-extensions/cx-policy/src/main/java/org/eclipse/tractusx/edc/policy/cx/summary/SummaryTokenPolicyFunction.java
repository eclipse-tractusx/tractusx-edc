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

package org.eclipse.tractusx.edc.policy.cx.summary;

import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.RequestScope;

import java.util.function.BiFunction;

import static java.lang.String.format;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.CX_SUMMARY_CREDENTIAL;

/**
 * Includes a summary credential in the token parameters.
 */
public class SummaryTokenPolicyFunction implements BiFunction<Policy, PolicyContext, Boolean> {

    @Override
    public Boolean apply(Policy policy, PolicyContext context) {
        var scopes = context.getContextData(RequestScope.Builder.class);
        if (scopes == null) {
            throw new EdcException(format("%s not set in policy context", RequestScope.Builder.class.getName()));
        }

        scopes.scope(CX_SUMMARY_CREDENTIAL);
        return true;
    }
}
