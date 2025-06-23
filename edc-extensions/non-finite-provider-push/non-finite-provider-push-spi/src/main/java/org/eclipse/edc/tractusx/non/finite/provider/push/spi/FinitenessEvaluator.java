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

package org.eclipse.edc.tractusx.non.finite.provider.push.spi;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

/**
 * Determines the finiteness of a data transfer
 */
public interface FinitenessEvaluator {

    /**
     * Evaluates if a {@link DataFlow} is non-finite
     *
     * @param dataflow dataflow to be evaluated
     * @return true if it is non-finite, false otherwise
     */
    boolean isNonFinite(DataFlow dataflow);

    /**
     * Evaluates if a {@link DataFlowStartMessage} represents a non-finite dataflow
     *
     * @param message dataflow start message to be evaluated
     * @return true if it is non-finite, false otherwise
     */
    boolean isNonFinite(DataFlowStartMessage message);
}
