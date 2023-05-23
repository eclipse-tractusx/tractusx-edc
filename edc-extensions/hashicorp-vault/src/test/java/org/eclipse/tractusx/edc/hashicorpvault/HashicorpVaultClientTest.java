/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.hashicorpvault;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.UUID;

class HashicorpVaultClientTest {
    private static final String KEY = "key";
    private static final String CUSTOM_SECRET_PATH = "v1/test/secret";
    private static final String HEALTH_PATH = "sys/health";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @SneakyThrows
    void getSecretValue() {
        // prepare
        String vaultUrl = "https://mock.url";
        String vaultToken = UUID.randomUUID().toString();
        HashicorpVaultClientConfig hashicorpVaultClientConfig =
                HashicorpVaultClientConfig.builder()
                        .vaultUrl(vaultUrl)
                        .vaultApiSecretPath(CUSTOM_SECRET_PATH)
                        .vaultApiHealthPath(HEALTH_PATH)
                        .isVaultApiHealthStandbyOk(false)
                        .vaultToken(vaultToken)
                        .timeout(TIMEOUT)
                        .build();

        OkHttpClient okHttpClient = Mockito.mock(OkHttpClient.class);
        HashicorpVaultClient vaultClient =
                new HashicorpVaultClient(hashicorpVaultClientConfig, okHttpClient, OBJECT_MAPPER);
        Call call = Mockito.mock(Call.class);
        Response response = Mockito.mock(Response.class);
        ResponseBody body = Mockito.mock(ResponseBody.class);
        HashicorpVaultGetEntryResponsePayload payload = new HashicorpVaultGetEntryResponsePayload();

        Mockito.when(okHttpClient.newCall(Mockito.any(Request.class))).thenReturn(call);
        Mockito.when(call.execute()).thenReturn(response);
        Mockito.when(response.code()).thenReturn(200);
        Mockito.when(response.body()).thenReturn(body);
        Mockito.when(body.string()).thenReturn(payload.toString());

        // invoke
        Result<String> result = vaultClient.getSecretValue(KEY);

        // verify
        Assertions.assertNotNull(result);
        Mockito.verify(okHttpClient, Mockito.times(1))
                .newCall(Mockito.argThat(request -> request.method().equalsIgnoreCase("GET") &&
                        request.url().encodedPath().contains(CUSTOM_SECRET_PATH + "/data") &&
                        request.url().encodedPathSegments().contains(KEY)));
    }

    @Test
    @SneakyThrows
    void setSecretValue() {
        // prepare
        String vaultUrl = "https://mock.url";
        String vaultToken = UUID.randomUUID().toString();
        String secretValue = UUID.randomUUID().toString();
        HashicorpVaultClientConfig hashicorpVaultClientConfig =
                HashicorpVaultClientConfig.builder()
                        .vaultUrl(vaultUrl)
                        .vaultApiSecretPath(CUSTOM_SECRET_PATH)
                        .vaultApiHealthPath(HEALTH_PATH)
                        .isVaultApiHealthStandbyOk(false)
                        .vaultToken(vaultToken)
                        .timeout(TIMEOUT)
                        .build();

        OkHttpClient okHttpClient = Mockito.mock(OkHttpClient.class);
        HashicorpVaultClient vaultClient =
                new HashicorpVaultClient(hashicorpVaultClientConfig, okHttpClient, OBJECT_MAPPER);
        HashicorpVaultCreateEntryResponsePayload payload =
                new HashicorpVaultCreateEntryResponsePayload();

        Call call = Mockito.mock(Call.class);
        Response response = Mockito.mock(Response.class);
        ResponseBody body = Mockito.mock(ResponseBody.class);

        Mockito.when(okHttpClient.newCall(Mockito.any(Request.class))).thenReturn(call);
        Mockito.when(call.execute()).thenReturn(response);
        Mockito.when(response.code()).thenReturn(200);
        Mockito.when(response.body()).thenReturn(body);
        Mockito.when(body.string()).thenReturn(payload.toString());

        // invoke
        Result<HashicorpVaultCreateEntryResponsePayload> result =
                vaultClient.setSecret(KEY, secretValue);

        // verify
        Assertions.assertNotNull(result);
        Mockito.verify(okHttpClient, Mockito.times(1))
                .newCall(
                        Mockito.argThat(
                                request ->
                                        request.method().equalsIgnoreCase("POST") &&
                                                request.url().encodedPath().contains(CUSTOM_SECRET_PATH + "/data") &&
                                                request.url().encodedPathSegments().contains(KEY)));
    }

    @Test
    @SneakyThrows
    void getHealth() {
        // prepare
        String vaultUrl = "https://mock.url";
        String vaultToken = UUID.randomUUID().toString();
        String secretValue = UUID.randomUUID().toString();
        HashicorpVaultClientConfig hashicorpVaultClientConfig =
                HashicorpVaultClientConfig.builder()
                        .vaultUrl(vaultUrl)
                        .vaultApiSecretPath(CUSTOM_SECRET_PATH)
                        .vaultApiHealthPath(HEALTH_PATH)
                        .isVaultApiHealthStandbyOk(false)
                        .vaultToken(vaultToken)
                        .timeout(TIMEOUT)
                        .build();

        OkHttpClient okHttpClient = Mockito.mock(OkHttpClient.class);
        HashicorpVaultClient vaultClient =
                new HashicorpVaultClient(hashicorpVaultClientConfig, okHttpClient, OBJECT_MAPPER);
        HashicorpVaultHealthResponsePayload payload = new HashicorpVaultHealthResponsePayload();

        Call call = Mockito.mock(Call.class);
        Response response = Mockito.mock(Response.class);
        ResponseBody body = Mockito.mock(ResponseBody.class);

        Mockito.when(okHttpClient.newCall(Mockito.any(Request.class))).thenReturn(call);
        Mockito.when(call.execute()).thenReturn(response);
        Mockito.when(response.code()).thenReturn(200);
        Mockito.when(response.body()).thenReturn(body);
        Mockito.when(body.string())
                .thenReturn(
                        "{ " +
                                "\"initialized\": true, " +
                                "\"sealed\": false," +
                                "\"standby\": false," +
                                "\"performance_standby\": false," +
                                "\"replication_performance_mode\": \"mode\"," +
                                "\"replication_dr_mode\": \"mode\"," +
                                "\"server_time_utc\": 100," +
                                "\"version\": \"1.0.0\"," +
                                "\"cluster_name\": \"name\"," +
                                "\"cluster_id\": \"id\" " +
                                " }");

        // invoke
        HashicorpVaultHealthResponse result = vaultClient.getHealth();

        // verify
        Assertions.assertNotNull(result);
        Mockito.verify(okHttpClient, Mockito.times(1))
                .newCall(
                        Mockito.argThat(
                                request ->
                                        request.method().equalsIgnoreCase("GET") &&
                                                request.url().encodedPath().contains(HEALTH_PATH) &&
                                                request.url().queryParameter("standbyok").equals("false") &&
                                                request.url().queryParameter("perfstandbyok").equals("false")));
        Assertions.assertEquals(200, result.getCode());
        Assertions.assertEquals(
                HashicorpVaultHealthResponse.HashiCorpVaultHealthResponseCode
                        .INITIALIZED_UNSEALED_AND_ACTIVE,
                result.getCodeAsEnum());

        HashicorpVaultHealthResponsePayload resultPayload = result.getPayload();

        Assertions.assertNotNull(resultPayload);
        Assertions.assertTrue(resultPayload.isInitialized());
        Assertions.assertFalse(resultPayload.isSealed());
        Assertions.assertFalse(resultPayload.isStandby());
        Assertions.assertFalse(resultPayload.isPerformanceStandby());
        Assertions.assertEquals("mode", resultPayload.getReplicationPerformanceMode());
        Assertions.assertEquals("mode", resultPayload.getReplicationDrMode());
        Assertions.assertEquals(100, resultPayload.getServerTimeUtc());
        Assertions.assertEquals("1.0.0", resultPayload.getVersion());
        Assertions.assertEquals("id", resultPayload.getClusterId());
        Assertions.assertEquals("name", resultPayload.getClusterName());
    }

    @Test
    @SneakyThrows
    void destroySecretValue() {
        // prepare
        String vaultUrl = "https://mock.url";
        String vaultToken = UUID.randomUUID().toString();
        HashicorpVaultClientConfig hashicorpVaultClientConfig =
                HashicorpVaultClientConfig.builder()
                        .vaultUrl(vaultUrl)
                        .vaultApiSecretPath(CUSTOM_SECRET_PATH)
                        .vaultApiHealthPath(HEALTH_PATH)
                        .isVaultApiHealthStandbyOk(false)
                        .vaultToken(vaultToken)
                        .timeout(TIMEOUT)
                        .build();

        OkHttpClient okHttpClient = Mockito.mock(OkHttpClient.class);
        HashicorpVaultClient vaultClient =
                new HashicorpVaultClient(hashicorpVaultClientConfig, okHttpClient, OBJECT_MAPPER);

        Call call = Mockito.mock(Call.class);
        Response response = Mockito.mock(Response.class);
        ResponseBody body = Mockito.mock(ResponseBody.class);
        Mockito.when(okHttpClient.newCall(Mockito.any(Request.class))).thenReturn(call);
        Mockito.when(call.execute()).thenReturn(response);
        Mockito.when(response.code()).thenReturn(200);
        Mockito.when(response.body()).thenReturn(body);

        // invoke
        Result<Void> result = vaultClient.destroySecret(KEY);

        // verify
        Assertions.assertNotNull(result);
        Mockito.verify(okHttpClient, Mockito.times(1))
                .newCall(
                        Mockito.argThat(
                                request ->
                                        request.method().equalsIgnoreCase("DELETE") &&
                                                request.url().encodedPath().contains(CUSTOM_SECRET_PATH + "/metadata") &&
                                                request.url().encodedPathSegments().contains(KEY)));
    }
}
