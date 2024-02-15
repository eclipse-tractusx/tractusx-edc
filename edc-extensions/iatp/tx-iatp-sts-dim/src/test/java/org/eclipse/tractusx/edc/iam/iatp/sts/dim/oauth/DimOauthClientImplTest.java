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

package org.eclipse.tractusx.edc.iam.iatp.sts.dim.oauth;

import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.tractusx.edc.iam.iatp.sts.dim.StsRemoteClientConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DimOauthClientImplTest {

    private final Oauth2Client oauth2Client = mock(Oauth2Client.class);

    private final Vault vault = mock(Vault.class);

    @Test
    void obtainRequestToken() {
        var config = new StsRemoteClientConfiguration("http://localhost:8081/token", "clientId", "client_secret_alias");
        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token("token").build();
        when(vault.resolveSecret("client_secret_alias")).thenReturn("client_secret");
        when(oauth2Client.requestToken(any())).thenReturn(Result.success(tokenRepresentation));
        var client = new DimOauthClientImpl(oauth2Client, vault, config);

        var response = client.obtainRequestToken();
        assertThat(response).isNotNull().extracting(Result::getContent).isEqualTo(tokenRepresentation);

        var captor = ArgumentCaptor.forClass(SharedSecretOauth2CredentialsRequest.class);
        verify(oauth2Client).requestToken(captor.capture());

        var request = captor.getValue();

        assertThat(request.getClientId()).isEqualTo(config.clientId());
        assertThat(request.getClientSecret()).isEqualTo("client_secret");
        assertThat(request.getUrl()).isEqualTo(config.tokenUrl());

    }

    @Test
    void obtainRequestToken_failed() {
        var config = new StsRemoteClientConfiguration("http://localhost:8081/token", "clientId", "client_secret");

        when(oauth2Client.requestToken(any())).thenReturn(Result.failure("failure"));
        var client = new DimOauthClientImpl(oauth2Client, vault, config);

        var response = client.obtainRequestToken();
        assertThat(response).isNotNull().matches(Result::failed);
    }
}
