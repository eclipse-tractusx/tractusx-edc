/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Added Test
 *
 */

package net.catenax.edc.dataplane.selector.configuration;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.dataspaceconnector.dataplane.selector.DataPlaneSelectorService;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

@ExtendWith(EdcExtension.class)
class DataPlaneSelectorConfigurationServiceExtensionEdcExtensionTest {

  private static final String S3_BUCKET = "s3-bucket";
  private static final String BLOB_STORAGE = "blob-storage";
  private static final String LOCAL_FILE_SYSTEM = "local-file-system";

  private static final String DATA_PLANE_INSTANCE_ID = "test-plane";
  private static final String DATA_PLANE_INSTANCE_URL = "http://127.0.0.1:8080/test";
  private static final String DATA_PLANE_INSTANCE_SOURCE_TYPES =
      String.format("%s, %s", S3_BUCKET, BLOB_STORAGE);
  private static final String DATA_PLANE_INSTANCE_DESTINATION_TYPES = LOCAL_FILE_SYSTEM;

  // mocks
  private DataPlaneSelectorService dataPlaneSelectorService;

  @BeforeEach
  final void beforeEach(EdcExtension extension) {
    dataPlaneSelectorService = Mockito.mock(DataPlaneSelectorService.class);

    extension.registerSystemExtension(ServiceExtension.class, new TestExtension());
    extension.setConfiguration(getConfig());
  }

  private Map<String, String> getConfig() {
    final String urlKey =
        String.format(
            "%s.%s.%s",
            DataPlaneSelectorConfigurationServiceExtension.CONFIG_PREFIX,
            DATA_PLANE_INSTANCE_ID,
            DataPlaneSelectorConfigurationServiceExtension.URL_SUFFIX);
    final String sourceTypesKey =
        String.format(
            "%s.%s.%s",
            DataPlaneSelectorConfigurationServiceExtension.CONFIG_PREFIX,
            DATA_PLANE_INSTANCE_ID,
            DataPlaneSelectorConfigurationServiceExtension.SOURCE_TYPES_SUFFIX);
    final String destinationTypesKey =
        String.format(
            "%s.%s.%s",
            DataPlaneSelectorConfigurationServiceExtension.CONFIG_PREFIX,
            DATA_PLANE_INSTANCE_ID,
            DataPlaneSelectorConfigurationServiceExtension.DESTINATION_TYPES_SUFFIX);
    final String propertiesKey =
        String.format(
            "%s.%s.%s",
            DataPlaneSelectorConfigurationServiceExtension.CONFIG_PREFIX,
            DATA_PLANE_INSTANCE_ID,
            DataPlaneSelectorConfigurationServiceExtension.PROPERTIES_SUFFIX);

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
  void registersDataPlaneInstance() {
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

  @Provides({DataPlaneSelectorService.class})
  private class TestExtension implements ServiceExtension {

    public void initialize(ServiceExtensionContext context) {
      context.registerService(
          org.eclipse.dataspaceconnector.dataplane.selector.DataPlaneSelectorService.class,
          dataPlaneSelectorService);
    }
  }
}
