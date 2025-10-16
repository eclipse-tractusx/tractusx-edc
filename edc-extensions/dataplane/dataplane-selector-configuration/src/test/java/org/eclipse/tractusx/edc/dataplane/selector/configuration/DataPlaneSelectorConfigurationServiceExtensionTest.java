/********************************************************************************
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.dataplane.selector.configuration;

import org.eclipse.edc.boot.system.DefaultServiceExtensionContext;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.boot.BootServicesExtension.PARTICIPANT_ID;
import static org.eclipse.tractusx.edc.dataplane.selector.configuration.DataPlaneSelectorConfigurationServiceExtension.CONFIG_PREFIX;
import static org.eclipse.tractusx.edc.dataplane.selector.configuration.DataPlaneSelectorConfigurationServiceExtension.DESTINATION_TYPES_SUFFIX;
import static org.eclipse.tractusx.edc.dataplane.selector.configuration.DataPlaneSelectorConfigurationServiceExtension.PROPERTIES_SUFFIX;
import static org.eclipse.tractusx.edc.dataplane.selector.configuration.DataPlaneSelectorConfigurationServiceExtension.PUBLIC_API_URL_PROPERTY;
import static org.eclipse.tractusx.edc.dataplane.selector.configuration.DataPlaneSelectorConfigurationServiceExtension.SOURCE_TYPES_SUFFIX;
import static org.eclipse.tractusx.edc.dataplane.selector.configuration.DataPlaneSelectorConfigurationServiceExtension.TRANSFER_TYPES_SUFFIX;
import static org.eclipse.tractusx.edc.dataplane.selector.configuration.DataPlaneSelectorConfigurationServiceExtension.URL_SUFFIX;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneSelectorConfigurationServiceExtensionTest {
    private static final String S3_BUCKET = "s3-bucket";
    private static final String BLOB_STORAGE = "blob-storage";

    private static final String DATA_PLANE_INSTANCE_ID = "test-plane";
    private static final String DATA_PLANE_INSTANCE_URL = "http://127.0.0.1:8080/test";
    private static final String DATA_PLANE_INSTANCE_SOURCE_TYPES = "%s, %s".formatted(S3_BUCKET, BLOB_STORAGE);
    private static final String DATA_PLANE_INSTANCE_TRANSFER_TYPES = "%s".formatted(S3_BUCKET);

    private static final String URL_KEY = "%s.%s".formatted(DATA_PLANE_INSTANCE_ID, URL_SUFFIX);
    private static final String SOURCE_TYPES_KEY = "%s.%s".formatted(DATA_PLANE_INSTANCE_ID, SOURCE_TYPES_SUFFIX);
    private static final String DESTINATION_TYPES_KEY = "%s.%s".formatted(DATA_PLANE_INSTANCE_ID, DESTINATION_TYPES_SUFFIX);
    private static final String TRANSFER_TYPES_KEY = "%s.%s".formatted(DATA_PLANE_INSTANCE_ID, TRANSFER_TYPES_SUFFIX);
    private static final String PROPERTIES_KEY = "%s.%s".formatted(DATA_PLANE_INSTANCE_ID, PROPERTIES_SUFFIX);

    private final DataPlaneSelectorService dataPlaneSelectorService = mock();
    private final Monitor monitor = mock();
    private DataPlaneSelectorConfigurationServiceExtension extension;

    @BeforeEach
    void setup(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(DataPlaneSelectorService.class, dataPlaneSelectorService);
        context.registerService(Monitor.class, monitor);

        extension = factory.constructInstance(DataPlaneSelectorConfigurationServiceExtension.class);
    }

    @Test
    void testName() {
        assertThat(extension.name()).contains("Data Plane Selector Configuration Extension");
    }

    @Test
    void testInitialize() {
        var config = ConfigFactory.fromMap(getConfig());
        var context = spy(contextWithConfig(config));

        extension.initialize(context);

        when(context.getConfig(CONFIG_PREFIX))
                .thenReturn(config);

        verify(dataPlaneSelectorService, times(1))
                .addInstance(argThat(dataPlaneInstance -> {
                    var s3Source = DataAddress.Builder.newInstance().type(S3_BUCKET).build();
                    var blobSource = DataAddress.Builder.newInstance().type(BLOB_STORAGE).build();

                    return dataPlaneInstance.getId().equals(DATA_PLANE_INSTANCE_ID) &&
                            dataPlaneInstance.getUrl().toString().equals(DATA_PLANE_INSTANCE_URL) &&
                            dataPlaneInstance.canHandle(s3Source, S3_BUCKET) &&
                            dataPlaneInstance.canHandle(blobSource, S3_BUCKET);
                }));
    }

    @ParameterizedTest
    @ArgumentsSource(MissingConfigArgumentsProvider.class)
    void testWarningOnPropertyMissing(String configKey, String configValue) {
        var configMap = getConfig();
        configMap.put(configKey, configValue);
        var config = ConfigFactory.fromMap(configMap);
        var context = contextWithConfig(config);

        extension.initialize(context);

        // one warning deprecation, one warning config missing, one warning data plane instance skipped
        verify(monitor, times(3)).warning(anyString());
    }

    @Test
    void throwsExceptionOnPropertiesNoJson() {
        var configMap = getConfig();
        configMap.put(CONFIG_PREFIX + "." + PROPERTIES_KEY, "no json");
        var config = ConfigFactory.fromMap(configMap);
        var context = contextWithConfig(config);

        assertThrows(EdcException.class, () -> extension.initialize(context));
    }

    @NotNull
    private DefaultServiceExtensionContext contextWithConfig(Config config) {
        var context = new DefaultServiceExtensionContext(monitor, config);
        context.initialize();
        return context;
    }

    private Map<String, String> getConfig() {
        return new HashMap<>() {
            {
                put(PARTICIPANT_ID, "participant");
                put(CONFIG_PREFIX + "." + URL_KEY, DATA_PLANE_INSTANCE_URL);
                put(CONFIG_PREFIX + "." + SOURCE_TYPES_KEY, DATA_PLANE_INSTANCE_SOURCE_TYPES);
                put(CONFIG_PREFIX + "." + TRANSFER_TYPES_KEY, DATA_PLANE_INSTANCE_TRANSFER_TYPES);
                put(CONFIG_PREFIX + "." + DESTINATION_TYPES_KEY, S3_BUCKET);
                put(CONFIG_PREFIX + "." + PROPERTIES_KEY, "{ \"%s\": \"%s\" }".formatted(PUBLIC_API_URL_PROPERTY, DATA_PLANE_INSTANCE_URL));
            }
        };
    }

    private static class MissingConfigArgumentsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(CONFIG_PREFIX + "." + URL_KEY, ""),
                    Arguments.of(CONFIG_PREFIX + "." + SOURCE_TYPES_KEY, ""),
                    Arguments.of(CONFIG_PREFIX + "." + DESTINATION_TYPES_KEY, ""),
                    Arguments.of(CONFIG_PREFIX + "." + PROPERTIES_KEY, "{}"));
        }
    }
}
