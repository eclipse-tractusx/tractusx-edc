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

package org.eclipse.tractusx.edc.dataplane.selector.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mockito;

class DataPlaneSelectorConfigurationServiceExtensionTest {
  private static final String S3_BUCKET = "s3-bucket";
  private static final String BLOB_STORAGE = "blob-storage";
  private static final String LOCAL_FILE_SYSTEM = "local-file-system";

  private static final String DATA_PLANE_INSTANCE_ID = "test-plane";
  private static final String DATA_PLANE_INSTANCE_URL = "http://127.0.0.1:8080/test";
  private static final String DATA_PLANE_INSTANCE_SOURCE_TYPES =
      String.format("%s, %s", S3_BUCKET, BLOB_STORAGE);
  private static final String DATA_PLANE_INSTANCE_DESTINATION_TYPES = LOCAL_FILE_SYSTEM;

  private static final String urlKey =
      String.format(
          "%s.%s",
          DATA_PLANE_INSTANCE_ID, DataPlaneSelectorConfigurationServiceExtension.URL_SUFFIX);
  private static final String sourceTypesKey =
      String.format(
          "%s.%s",
          DATA_PLANE_INSTANCE_ID,
          DataPlaneSelectorConfigurationServiceExtension.SOURCE_TYPES_SUFFIX);
  private static final String destinationTypesKey =
      String.format(
          "%s.%s",
          DATA_PLANE_INSTANCE_ID,
          DataPlaneSelectorConfigurationServiceExtension.DESTINATION_TYPES_SUFFIX);
  private static final String propertiesKey =
      String.format(
          "%s.%s",
          DATA_PLANE_INSTANCE_ID, DataPlaneSelectorConfigurationServiceExtension.PROPERTIES_SUFFIX);

  private DataPlaneSelectorConfigurationServiceExtension extension;

  // mocks
  private ServiceExtensionContext serviceExtensionContext;
  private DataPlaneSelectorService dataPlaneSelectorService;
  private Monitor monitor;

  @BeforeEach
  void setup() {
    extension = new DataPlaneSelectorConfigurationServiceExtension();

    serviceExtensionContext = Mockito.mock(ServiceExtensionContext.class);
    dataPlaneSelectorService = Mockito.mock(DataPlaneSelectorService.class);
    monitor = Mockito.mock(Monitor.class);

    Mockito.when(serviceExtensionContext.getService(DataPlaneSelectorService.class))
        .thenReturn(dataPlaneSelectorService);
    Mockito.when(serviceExtensionContext.getMonitor()).thenReturn(monitor);
  }

  private Map<String, String> getConfig() {
    return new HashMap<>() {
      {
        put(urlKey, DATA_PLANE_INSTANCE_URL);
        put(sourceTypesKey, DATA_PLANE_INSTANCE_SOURCE_TYPES);
        put(destinationTypesKey, DATA_PLANE_INSTANCE_DESTINATION_TYPES);
        put(
            propertiesKey,
            String.format(
                "{ \"%s\": \"%s\" }",
                DataPlaneSelectorConfigurationServiceExtension.PUBLIC_API_URL_PROPERTY,
                DATA_PLANE_INSTANCE_URL));
      }
    };
  }

  @Test
  void testName() {
    final DataPlaneSelectorConfigurationServiceExtension extension =
        new DataPlaneSelectorConfigurationServiceExtension();

    Assertions.assertNotNull(extension.name());
    Assertions.assertEquals("Data Plane Selector Configuration Extension", extension.name());
  }

  @Test
  void testInitialize() {

    final Config config = ConfigFactory.fromMap(getConfig());

    Mockito.when(serviceExtensionContext.getConfig("edc.dataplane.selector")).thenReturn(config);

    extension.initialize(serviceExtensionContext);

    Mockito.verify(serviceExtensionContext, Mockito.times(1))
        .getService(DataPlaneSelectorService.class);
    Mockito.verify(serviceExtensionContext, Mockito.times(1)).getMonitor();
    Mockito.when(
            serviceExtensionContext.getConfig(
                DataPlaneSelectorConfigurationServiceExtension.CONFIG_PREFIX))
        .thenReturn(config);

    Mockito.verify(dataPlaneSelectorService, Mockito.times(1))
        .addInstance(
            Mockito.argThat(
                dataPlaneInstance -> {
                  final DataAddress s3Source =
                      DataAddress.Builder.newInstance().type(S3_BUCKET).build();
                  final DataAddress blobSource =
                      DataAddress.Builder.newInstance().type(BLOB_STORAGE).build();
                  final DataAddress fsSink =
                      DataAddress.Builder.newInstance().type(LOCAL_FILE_SYSTEM).build();

                  final boolean matchingId =
                      dataPlaneInstance.getId().equals(DATA_PLANE_INSTANCE_ID);
                  final boolean matchingUrl =
                      dataPlaneInstance.getUrl().toString().equals(DATA_PLANE_INSTANCE_URL);
                  final boolean matchingCanHandleS3ToFileSystem =
                      dataPlaneInstance.canHandle(s3Source, fsSink);
                  final boolean matchingCanHandleBlobToFileSystem =
                      dataPlaneInstance.canHandle(blobSource, fsSink);

                  if (!matchingId)
                    System.err.printf(
                        "Expected ID %s, but got %s%n",
                        DATA_PLANE_INSTANCE_ID, dataPlaneInstance.getId());
                  if (!matchingUrl)
                    System.err.printf(
                        "Expected URL %s, but got %s%n",
                        DATA_PLANE_INSTANCE_URL, dataPlaneInstance.getUrl());
                  if (!matchingCanHandleS3ToFileSystem)
                    System.err.printf(
                        "Expected Instance to be handle source %s and sink %s%n",
                        S3_BUCKET, LOCAL_FILE_SYSTEM);
                  if (!matchingCanHandleBlobToFileSystem)
                    System.err.printf(
                        "Expected Instance to be handle source %s and sink %s%n",
                        BLOB_STORAGE, LOCAL_FILE_SYSTEM);

                  return matchingId
                      && matchingUrl
                      && matchingCanHandleS3ToFileSystem
                      && matchingCanHandleBlobToFileSystem;
                }));
  }

  @ParameterizedTest
  @ArgumentsSource(MissingConfigArgumentsProvider.class)
  void testWarningOnPropertyMissing(String configKey, String configValue) {
    Map<String, String> configMap = getConfig();
    configMap.put(configKey, configValue);

    final Config config = ConfigFactory.fromMap(configMap);

    Mockito.when(
            serviceExtensionContext.getConfig(
                DataPlaneSelectorConfigurationServiceExtension.CONFIG_PREFIX))
        .thenReturn(config);

    extension.initialize(serviceExtensionContext);

    // one warning config missing, one warning data plane instance skipped
    Mockito.verify(monitor, Mockito.times(2)).warning(Mockito.anyString());
  }

  @Test
  void throwsExceptionOnPropertiesNoJson() {
    Map<String, String> configMap = getConfig();
    configMap.put(propertiesKey, "no json");

    final Config config = ConfigFactory.fromMap(configMap);

    Mockito.when(
            serviceExtensionContext.getConfig(
                DataPlaneSelectorConfigurationServiceExtension.CONFIG_PREFIX))
        .thenReturn(config);

    Assertions.assertThrows(
        EdcException.class, () -> extension.initialize(serviceExtensionContext));
  }

  private static class MissingConfigArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          Arguments.of(urlKey, ""),
          Arguments.of(sourceTypesKey, ""),
          Arguments.of(destinationTypesKey, ""),
          Arguments.of(propertiesKey, "{}"));
    }
  }
}
