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

package org.eclipse.tractusx.edc.policy.cx.usage;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.policy.cx.common.ValueValidatingConstraintFunction;

import java.util.Set;

/**
 * This is a placeholder constraint function for UsagePurpose. It always returns true but allows
 * the validation of policies to be strictly enforced.
 */
public class UsagePurposeConstraintFunction<C extends ParticipantAgentPolicyContext> extends ValueValidatingConstraintFunction<C> {
    public static final String USAGE_PURPOSE = "UsagePurpose";

    public UsagePurposeConstraintFunction() {
        super(
                Set.of(Operator.IS_ALL_OF),
                Set.of(
                        "cx.core.legalRequirementForThirdparty:1",
                        "cx.core.industrycore:1",
                        "cx.core.qualityNotifications:1",
                        "cx.core.digitalTwinRegistry:1",
                        "cx.pcf.base:1",
                        "cx.quality.base:1",
                        "cx.dcm.base:1",
                        "cx.puris.base:1",
                        "cx.circular.dpp:1",
                        "cx.circular.smc:1",
                        "cx.circular.marketplace:1",
                        "cx.circular.materialaccounting:1",
                        "cx.bpdm.gate.upload:1",
                        "cx.bpdm.gate.download:1",
                        "cx.bpdm.pool:1",
                        "cx.bpdm.vas.countryrisk:1",
                        "cx.bpdm.vas.dataquality.upload:1",
                        "cx.bpdm.vas.dataquality.download:1",
                        "cx.bpdm.vas.bdv.upload:1",
                        "cx.bpdm.vas.bdv.download:1",
                        "cx.bpdm.vas.fpd.upload:1",
                        "cx.bpdm.vas.fpd.download:1",
                        "cx.bpdm.vas.swd.upload:1",
                        "cx.bpdm.vas.swd.download:1",
                        "cx.bpdm.vas.nps.upload:1",
                        "cx.bpdm.vas.nps.download:1",
                        "cx.ccm.base:1",
                        "cx.bpdm.poolAll:1",
                        "cx.logistics.base:1"
                ),
                true
        );
    }
}
