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

package org.eclipse.tractusx.edc.iatp.policy;

import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.model.Permission;

import java.util.Map;

public abstract class BaseCredentialEvaluationFunction implements AtomicConstraintFunction<Permission> {

    protected <T> T getClaim(Class<T> type, String postfix, Map<String, Object> claims) {
        return claims.entrySet().stream().filter(e -> e.getKey().endsWith(postfix))
                .findFirst()
                .map(Map.Entry::getValue)
                .map(type::cast)
                .orElse(null);
    }
}
