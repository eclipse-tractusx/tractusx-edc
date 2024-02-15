/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.iam.iatp.sts.dim;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.iam.iatp.sts.dim.DimStsConfigurationExtension.CLIENT_ID;
import static org.eclipse.tractusx.edc.iam.iatp.sts.dim.DimStsConfigurationExtension.CLIENT_SECRET_ALIAS;
import static org.eclipse.tractusx.edc.iam.iatp.sts.dim.DimStsConfigurationExtension.TOKEN_URL;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class DimStsConfigurationExtensionTest {

    @Test
    void initialize(ServiceExtensionContext context, DimStsConfigurationExtension extension) {
        var config = mock(Config.class);
        when(context.getConfig()).thenReturn(config);
        when(config.getString(TOKEN_URL)).thenReturn("url");
        when(config.getString(CLIENT_ID)).thenReturn("clientId");
        when(config.getString(CLIENT_SECRET_ALIAS)).thenReturn("clientSecretAlias");

        assertThat(extension.clientConfiguration(context)).satisfies(stsConfig -> {
            assertThat(stsConfig.clientId()).isEqualTo("clientId");
            assertThat(stsConfig.clientSecretAlias()).isEqualTo("clientSecretAlias");
            assertThat(stsConfig.tokenUrl()).isEqualTo("url");
        });
    }


}
