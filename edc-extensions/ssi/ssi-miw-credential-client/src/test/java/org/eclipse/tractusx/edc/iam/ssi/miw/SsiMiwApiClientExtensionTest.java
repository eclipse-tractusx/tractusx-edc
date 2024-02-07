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

package org.eclipse.tractusx.edc.iam.ssi.miw;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClient;
import org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClientImpl;
import org.eclipse.tractusx.edc.iam.ssi.miw.config.SsiMiwConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class SsiMiwApiClientExtensionTest {

    private final SsiMiwConfiguration cfg = mock(SsiMiwConfiguration.class);
    private SsiMiwApiClientExtension extension;

    @BeforeEach
    void setup(ObjectFactory factory, ServiceExtensionContext context) {
        context.registerService(SsiMiwConfiguration.class, cfg);
        context.registerService(MiwApiClient.class, mock(MiwApiClient.class));
        context.registerService(TypeManager.class, new TypeManager());
        extension = factory.constructInstance(SsiMiwApiClientExtension.class);
    }

    @Test
    void initialize(ServiceExtensionContext context) {
        when(cfg.getUrl()).thenReturn("http://localhost");
        when(cfg.getAuthorityId()).thenReturn("id");

        assertThat(extension.apiClient(context)).isInstanceOf(MiwApiClientImpl.class);

        verify(cfg).getUrl();
        verify(cfg).getAuthorityId();

    }
}
