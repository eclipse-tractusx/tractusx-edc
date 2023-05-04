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

package org.eclipse.tractusx.edc.spi.cp.adapter.service;

import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.tractusx.edc.spi.cp.adapter.types.TransferOpenRequest;

/**
 * Service for opening a transfer process.
 */
public interface AdapterTransferProcessService {

    /**
     * Open a transfer process by firing a contract negotiation. Implementors should fire a contract negotiation
     * and automatically fire a transfer process once the agreement has been reached.
     *
     * @param request The open request
     * @return The result
     */
    ServiceResult<Void> openTransfer(TransferOpenRequest request);
}
