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

package org.eclipse.tractusx.edc.non.finite.provider.push.core.evaluator;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinitenessEvaluatorImplTest {

    private FinitenessEvaluatorImpl finitenessEvaluator;

    @BeforeEach
    public void setup() {
        finitenessEvaluator = new FinitenessEvaluatorImpl();
    }

    private DataFlow createFiniteDataFlow() {
        return DataFlow.Builder.newInstance()
                .source(DataAddress.Builder.newInstance().type("smt").build())
                .build();
    }

    private DataFlow createNonFiniteDataFlow() {
        return DataFlow.Builder.newInstance()
                .source(DataAddress.Builder.newInstance()
                        .type("smt")
                        .property(EDC_NAMESPACE + "isNonFinite", "true")
                        .build())
                .build();
    }

    @Test
    public void isNonFinite_shouldReturnFalse_whenDataFlowIsFinite() {
        assertFalse(finitenessEvaluator.isNonFinite(createFiniteDataFlow()));
    }

    @Test
    public void isNonFinite_shouldReturnTrue_whenDataFlowIsNonFinite() {
        assertTrue(finitenessEvaluator.isNonFinite(createNonFiniteDataFlow()));
    }

    @Test
    public void isNonFinite_shouldReturnFalse_whenDataFlowStartMessageIsFinite() {
        assertFalse(finitenessEvaluator.isNonFinite(createFiniteDataFlow().toRequest()));
    }

    @Test
    public void isNonFinite_shouldReturnTrue_whenDataFlowStartMessageIsNonFinite() {
        assertTrue(finitenessEvaluator.isNonFinite(createNonFiniteDataFlow().toRequest()));
    }

}
