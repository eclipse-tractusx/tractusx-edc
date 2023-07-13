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

package org.eclipse.tractusx.edc.edr.spi;

import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.tractusx.edc.edr.spi.types.NegotiateEdrRequest;

/**
 * Manages EDRs lifecycle
 */
@ExtensionPoint
public interface EdrManager {

    /**
     * Initiated a new EDR negotiation. An EDR negotiation consists on two sub-processes. Contract negotiation and transfer
     * request. Once the latter is completed the returned EDR from the provided will be store in the EDR cache for consumption
     *
     * @param request Request Data
     * @return The contract negotiation
     */
    StatusResult<ContractNegotiation> initiateEdrNegotiation(NegotiateEdrRequest request);

}
