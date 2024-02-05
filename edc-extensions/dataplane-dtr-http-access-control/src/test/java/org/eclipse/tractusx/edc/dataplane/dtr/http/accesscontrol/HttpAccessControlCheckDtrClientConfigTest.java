/********************************************************************************
 * Copyright (c) 2024 Robert Bosch Manufacturing Solutions GmbH and others
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol;

import org.eclipse.edc.spi.system.configuration.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.ASPECT_MODEL_URL_PATTERN;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.DTR_ACCESS_VERIFICATION_URL;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.OAUTH2_TOKEN_CLIENT_ID;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.OAUTH2_TOKEN_CLIENT_SECRET_PATH;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.OAUTH2_TOKEN_ENDPOINT_URL;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.OAUTH2_TOKEN_SCOPE;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpAccessControlCheckDtrClientConfigTest {

    private Config config;
    private HttpAccessControlCheckDtrClientConfig underTest;

    @BeforeEach
    void setUp() {
        config = mock();
    }

    @Test
    void test_GetAspectModelUrlPattern_ShouldReturnExpectedValue_WhenConfigurationWasSet() {
        //given
        final String expected = "http://aspec-model/api";
        when(config.getString(eq(ASPECT_MODEL_URL_PATTERN), isNull())).thenReturn(expected);
        underTest = new HttpAccessControlCheckDtrClientConfig(config);

        //when
        final String actual = underTest.getAspectModelUrlPattern();

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void test_GetDtrAccessVerificationUrl_ShouldReturnExpectedValue_WhenConfigurationWasSet() {
        //given
        final String expected = "http://dtr/submodel-descriptor/authorized";
        when(config.getString(eq(DTR_ACCESS_VERIFICATION_URL), isNull())).thenReturn(expected);
        underTest = new HttpAccessControlCheckDtrClientConfig(config);

        //when
        final String actual = underTest.getDtrAccessVerificationUrl();

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void test_GetOauth2TokenEndpointUrl_ShouldReturnExpectedValue_WhenConfigurationWasSet() {
        //given
        final String expected = "http://oauth2/token";
        when(config.getString(eq(OAUTH2_TOKEN_ENDPOINT_URL), isNull())).thenReturn(expected);
        underTest = new HttpAccessControlCheckDtrClientConfig(config);

        //when
        final String actual = underTest.getOauth2TokenEndpointUrl();

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void test_GetOauth2TokenScope_ShouldReturnExpectedValue_WhenConfigurationWasSet() {
        //given
        final String expected = "aud:dtr-id";
        when(config.getString(eq(OAUTH2_TOKEN_SCOPE), isNull())).thenReturn(expected);
        underTest = new HttpAccessControlCheckDtrClientConfig(config);

        //when
        final String actual = underTest.getOauth2TokenScope();

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void test_GetOauth2ClientId_ShouldReturnExpectedValue_WhenConfigurationWasSet() {
        //given
        final String expected = "edc-client-id";
        when(config.getString(eq(OAUTH2_TOKEN_CLIENT_ID), isNull())).thenReturn(expected);
        underTest = new HttpAccessControlCheckDtrClientConfig(config);

        //when
        final String actual = underTest.getOauth2ClientId();

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void test_GetOauth2ClientSecretPath_ShouldReturnExpectedValue_WhenConfigurationWasSet() {
        //given
        final String expected = "edc-client-secret-path";
        when(config.getString(eq(OAUTH2_TOKEN_CLIENT_SECRET_PATH), isNull())).thenReturn(expected);
        underTest = new HttpAccessControlCheckDtrClientConfig(config);

        //when
        final String actual = underTest.getOauth2ClientSecretPath();

        //then
        assertThat(actual).isEqualTo(expected);
    }
}