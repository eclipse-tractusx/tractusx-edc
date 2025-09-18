/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.interceptor;

import okhttp3.OkHttpClient;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class OkHttpInterceptorExtensionTest {

    private final OkHttpClient okHttpClient = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        when(okHttpClient.newBuilder()).thenReturn(new OkHttpClient.Builder());
        context.registerService(OkHttpClient.class, okHttpClient);
    }

    @Test
    void okHttpClient_shouldAddInterceptor(OkHttpInterceptorExtension extension) {
        var client = extension.okHttpClient();

        org.assertj.core.api.Assertions.assertThat(client.interceptors())
                .hasSize(1)
                .first()
                .isInstanceOf(OkHttpInterceptor.class);
    }
}
