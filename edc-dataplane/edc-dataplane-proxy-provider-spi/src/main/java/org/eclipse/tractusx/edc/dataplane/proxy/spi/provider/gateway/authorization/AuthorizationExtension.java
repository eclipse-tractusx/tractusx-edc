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

package org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.authorization;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;

/**
 * Performs an authorization check for the given path against a set of claims.
 */
public interface AuthorizationExtension {

    /**
     * Performs an authorization check for the given path against the presented claims. The path is the request alias path, not
     * the proxied path.
     *
     * @param token the validated claim token
     * @param path the request alias path, not the dereferenced proxied path
     */
    Result<Void> authorize(ClaimToken token, String path);

}
