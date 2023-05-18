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

package org.eclipse.tractusx.edc.dataplane.proxy.provider.api.response;

import org.junit.jupiter.api.Test;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.dataplane.proxy.provider.api.response.ResponseHelper.createMessageResponse;

class ResponseHelperTest {

    @Test
    void verify_responses() {
        assertThat(createMessageResponse(INTERNAL_SERVER_ERROR, "Some error", APPLICATION_JSON_TYPE).getEntity()).isEqualTo("'Some error'");
        assertThat(createMessageResponse(INTERNAL_SERVER_ERROR, "Some error", TEXT_PLAIN_TYPE).getEntity()).isEqualTo("Some error");
    }
}
