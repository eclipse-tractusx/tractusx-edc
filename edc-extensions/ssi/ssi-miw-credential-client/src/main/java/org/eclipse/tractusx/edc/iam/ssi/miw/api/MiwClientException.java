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

import okhttp3.Response;
import org.eclipse.edc.spi.http.EdcHttpClientException;

/**
 * Custom client exception for handling failure and retries when fetching data from MIW.
 */
public class MiwClientException extends EdcHttpClientException {
    private final Response response;

    public MiwClientException(String message) {
        this(message, null);
    }

    public MiwClientException(String message, Response response) {
        super(message);
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }
}
