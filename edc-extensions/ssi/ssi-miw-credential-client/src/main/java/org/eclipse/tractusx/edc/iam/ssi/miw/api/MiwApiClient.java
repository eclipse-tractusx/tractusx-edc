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

package org.eclipse.tractusx.edc.iam.ssi.miw.api;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;

import java.util.List;
import java.util.Map;
import java.util.Set;

@ExtensionPoint
public interface MiwApiClient {
    String VP = "vp";

    Result<List<Map<String, Object>>> getCredentials(Set<String> types, String holderIdentifier);

    Result<Map<String, Object>> createPresentation(List<Map<String, Object>> credentials, String holderIdentifier);

    Result<Void> verifyPresentation(String jwtPresentation);

}
