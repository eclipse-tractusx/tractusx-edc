/********************************************************************************
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.nonfiniteproviderpush.core.evaluator;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.tractusx.non.finite.provider.push.spi.FinitenessEvaluator;

import static java.lang.Boolean.parseBoolean;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class FinitenessEvaluatorImpl implements FinitenessEvaluator {

    private static final String IS_NON_FINITE_PROP = EDC_NAMESPACE + "isNonFinite";

    @Override
    public boolean isNonFinite(DataFlow dataflow) {
        return containsNonFiniteProperty(dataflow.getSource());
    }

    @Override
    public boolean isNonFinite(DataFlowStartMessage message) {
        return containsNonFiniteProperty(message.getSourceDataAddress());
    }

    private boolean containsNonFiniteProperty(DataAddress address) {
        return parseBoolean(address.getStringProperty(IS_NON_FINITE_PROP));
    }
}
