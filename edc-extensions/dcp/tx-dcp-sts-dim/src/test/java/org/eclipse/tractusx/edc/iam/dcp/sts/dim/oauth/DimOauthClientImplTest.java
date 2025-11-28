/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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
 */

package org.eclipse.tractusx.edc.iam.dcp.sts.dim.oauth;

import org.eclipse.edc.iam.decentralizedclaims.sts.remote.StsRemoteClientConfiguration;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DimOauthClientImplTest {

    private final Oauth2Client oauth2Client = mock();
    private final Vault vault = mock();
    private final Monitor monitor = mock();
    private final ParticipantContextSupplier participantContextSupplier = () -> ServiceResult.success(
            ParticipantContext.Builder.newInstance().participantContextId("participantContextId").identity("identity").build());

    @Test
    void obtainRequestToken_withNoExpiration() {
        var config = new StsRemoteClientConfiguration("http://localhost:8081/token", "clientId", "client_secret_alias");
        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token("token").build();
        when(vault.resolveSecret("participantContextId", "client_secret_alias")).thenReturn("client_secret");
        when(oauth2Client.requestToken(any())).thenReturn(Result.success(tokenRepresentation));
        var client = new DimOauthClientImpl(oauth2Client, vault, config, Clock.systemUTC(), monitor, participantContextSupplier);

        var response = client.obtainRequestToken();
        assertThat(response).isNotNull().extracting(Result::getContent).isEqualTo(tokenRepresentation);

        var captor = ArgumentCaptor.forClass(SharedSecretOauth2CredentialsRequest.class);
        verify(oauth2Client).requestToken(captor.capture());

        var request = captor.getValue();

        assertThat(request.getClientId()).isEqualTo(config.clientId());
        assertThat(request.getClientSecret()).isEqualTo("client_secret");
        assertThat(request.getUrl()).isEqualTo(config.tokenUrl());

        response = client.obtainRequestToken();
        assertThat(response).isNotNull().extracting(Result::getContent).isEqualTo(tokenRepresentation);

        verify(oauth2Client, times(2)).requestToken(any());
    }

    @Test
    void obtainRequestToken_withExpiration_whenNotExpired() {
        var config = new StsRemoteClientConfiguration("http://localhost:8081/token", "clientId", "client_secret_alias");
        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token("token").expiresIn(10L).build();
        when(vault.resolveSecret("participantContextId", "client_secret_alias")).thenReturn("client_secret");
        when(oauth2Client.requestToken(any())).thenReturn(Result.success(tokenRepresentation));
        var client = new DimOauthClientImpl(oauth2Client, vault, config, Clock.systemUTC(), monitor, participantContextSupplier);

        var response = client.obtainRequestToken();
        assertThat(response).isNotNull().extracting(Result::getContent).isEqualTo(tokenRepresentation);

        var captor = ArgumentCaptor.forClass(SharedSecretOauth2CredentialsRequest.class);
        verify(oauth2Client).requestToken(captor.capture());

        var request = captor.getValue();

        assertThat(request.getClientId()).isEqualTo(config.clientId());
        assertThat(request.getClientSecret()).isEqualTo("client_secret");
        assertThat(request.getUrl()).isEqualTo(config.tokenUrl());

        response = client.obtainRequestToken();
        assertThat(response).isNotNull().extracting(Result::getContent).isEqualTo(tokenRepresentation);

        verify(oauth2Client, times(1)).requestToken(any());
    }

    @Test
    void obtainRequestToken_withExpiration_whenExpired() throws InterruptedException {
        var config = new StsRemoteClientConfiguration("http://localhost:8081/token", "clientId", "client_secret_alias");
        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token("token").expiresIn(2L).build();
        when(vault.resolveSecret("participantContextId", "client_secret_alias")).thenReturn("client_secret");
        when(oauth2Client.requestToken(any())).thenReturn(Result.success(tokenRepresentation));
        var client = new DimOauthClientImpl(oauth2Client, vault, config, Clock.systemUTC(), monitor, participantContextSupplier);

        var response = client.obtainRequestToken();
        assertThat(response).isNotNull().extracting(Result::getContent).isEqualTo(tokenRepresentation);

        var captor = ArgumentCaptor.forClass(SharedSecretOauth2CredentialsRequest.class);
        verify(oauth2Client).requestToken(captor.capture());

        var request = captor.getValue();

        assertThat(request.getClientId()).isEqualTo(config.clientId());
        assertThat(request.getClientSecret()).isEqualTo("client_secret");
        assertThat(request.getUrl()).isEqualTo(config.tokenUrl());

        Thread.sleep(2100);

        response = client.obtainRequestToken();
        assertThat(response).isNotNull().extracting(Result::getContent).isEqualTo(tokenRepresentation);

        verify(oauth2Client, times(2)).requestToken(any());
    }

    @Test
    void obtainRequestToken_failed() {
        var config = new StsRemoteClientConfiguration("http://localhost:8081/token", "clientId", "client_secret");

        when(oauth2Client.requestToken(any())).thenReturn(Result.failure("failure"));
        var client = new DimOauthClientImpl(oauth2Client, vault, config, Clock.systemUTC(), monitor, participantContextSupplier);

        var response = client.obtainRequestToken();
        assertThat(response).isNotNull().matches(Result::failed);
    }
}
