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

import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.DEFAULT_PORT;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.EDC_DATA_PLANE_BASE_URL;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.EDC_DTR_CONFIG_NAMES;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.EDC_DTR_CONFIG_PREFIX;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.ERROR_ENDPOINT_PORT;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpAccessControlCheckClientConfigTest {

    private ServiceExtensionContext serviceExtensionContext;
    private HttpAccessControlCheckClientConfig underTest;

    @BeforeEach
    void setUp() {
        serviceExtensionContext = mock();
    }

    @Test
    void test_GetErrorEndpointPort_ShouldReturnExpectedValue_WhenConfigurationWasSet() {
        //given
        final int expected = 1;
        when(serviceExtensionContext.getSetting(EDC_DTR_CONFIG_NAMES, "")).thenReturn("name");
        when(serviceExtensionContext.getSetting(ERROR_ENDPOINT_PORT, DEFAULT_PORT)).thenReturn(expected);
        when(serviceExtensionContext.getConfig(EDC_DTR_CONFIG_PREFIX + "name")).thenReturn(mock());
        underTest = new HttpAccessControlCheckClientConfig(serviceExtensionContext);

        //when
        final int actual = underTest.getErrorEndpointPort();

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void test_GetDtrClientConfigMap_ShouldReturnEmptyMap_WhenConfigurationIsNotSet() {
        //given
        final int expected = 1;
        when(serviceExtensionContext.getSetting(EDC_DTR_CONFIG_NAMES, "")).thenReturn("");
        when(serviceExtensionContext.getSetting(ERROR_ENDPOINT_PORT, DEFAULT_PORT)).thenReturn(expected);
        underTest = new HttpAccessControlCheckClientConfig(serviceExtensionContext);

        //when
        final var actual = underTest.getDtrClientConfigMap();

        //then
        assertThat(actual).isEqualTo(Map.of());
        verify(serviceExtensionContext, never()).getConfig(anyString());
    }

    @Test
    void test_GetErrorEndpointBaseUrl_ShouldReturnExpectedValue_WhenConfigurationWasSet() {
        //given
        final String expected = "http://localhost:" + DEFAULT_PORT + "/error";
        when(serviceExtensionContext.getSetting(EDC_DTR_CONFIG_NAMES, "")).thenReturn("name");
        when(serviceExtensionContext.getSetting(ERROR_ENDPOINT_PORT, DEFAULT_PORT)).thenReturn(DEFAULT_PORT);
        when(serviceExtensionContext.getConfig(EDC_DTR_CONFIG_PREFIX + "name")).thenReturn(mock());
        underTest = new HttpAccessControlCheckClientConfig(serviceExtensionContext);

        //when
        final String actual = underTest.getErrorEndpointBaseUrl();

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void test_GetErrorEndpointPathForStatus_ShouldReturnExpectedValue_WhenCalledWithValidValue() {
        //given
        final int status = 401;
        final String expected = "/http/access/error/401";
        when(serviceExtensionContext.getSetting(EDC_DTR_CONFIG_NAMES, "")).thenReturn("name");
        when(serviceExtensionContext.getConfig(EDC_DTR_CONFIG_PREFIX + "name")).thenReturn(mock());
        underTest = new HttpAccessControlCheckClientConfig(serviceExtensionContext);

        //when
        final String actual = underTest.getErrorEndpointPathForStatus(status);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void test_GetReasonPhraseParameterName_ShouldReturnExpectedValue_WhenCalled() {
        //given
        final String expected = "reasonPhrase";
        when(serviceExtensionContext.getSetting(EDC_DTR_CONFIG_NAMES, "")).thenReturn("name");
        when(serviceExtensionContext.getConfig(EDC_DTR_CONFIG_PREFIX + "name")).thenReturn(mock());
        underTest = new HttpAccessControlCheckClientConfig(serviceExtensionContext);

        //when
        final String actual = underTest.getReasonPhraseParameterName();

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void test_GetEdcDataPlaneBaseUrl_ShouldReturnExpectedValue_WhenConfigurationWasSet() {
        //given
        final String expected = "http://edc-data-plane/proxy";
        when(serviceExtensionContext.getSetting(EDC_DTR_CONFIG_NAMES, "")).thenReturn("name");
        when(serviceExtensionContext.getSetting(eq(EDC_DATA_PLANE_BASE_URL), isNull())).thenReturn(expected);
        when(serviceExtensionContext.getConfig(EDC_DTR_CONFIG_PREFIX + "name")).thenReturn(mock());
        underTest = new HttpAccessControlCheckClientConfig(serviceExtensionContext);

        //when
        final String actual = underTest.getEdcDataPlaneBaseUrl();

        //then
        assertThat(actual).isEqualTo(expected);
    }
}