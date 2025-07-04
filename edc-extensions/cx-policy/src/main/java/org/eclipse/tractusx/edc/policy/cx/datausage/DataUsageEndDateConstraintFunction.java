/*
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
 */

package org.eclipse.tractusx.edc.policy.cx.datausage;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.policy.cx.common.ValueValidatingConstraintFunction;

import java.util.Set;

/**
 * This is a placeholder constraint function for DataUsageEndDate. It always returns true but allows
 * the validation of policies to be strictly enforced.
 */
public class DataUsageEndDateConstraintFunction<C extends ParticipantAgentPolicyContext> extends ValueValidatingConstraintFunction<C> {
    public static final String DATA_USAGE_END_DATE = "DataUsageEndDate";

    public DataUsageEndDateConstraintFunction() {
        super(
                Set.of(Operator.EQ),
                "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(Z|[+-]\\d{2}:\\d{2}))$"
        );
    }
}
