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

package org.eclipse.tractusx.edc.policy.cx.common;

/**
 * Defines standard EDC policy scopes.
 */
public interface PolicyScopes {
    String CATALOG_REQUEST_SCOPE = "catalog.request";
    String NEGOTIATION_REQUEST_SCOPE = "contract.negotiation.request";
    String TRANSFER_PROCESS_REQUEST_SCOPE = "transfer.process.request";

    String CATALOG_SCOPE = "contract.cataloging";
    String NEGOTIATION_SCOPE = "contract.negotiation";
    String TRANSFER_PROCESS_SCOPE = "transfer.process";
}
