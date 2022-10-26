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
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package org.eclipse.tractusx.edc.boot;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.dataspaceconnector.boot.system.DependencyGraph;
import org.eclipse.dataspaceconnector.boot.system.ServiceLocator;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Setting;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.configuration.Config;
import org.eclipse.dataspaceconnector.spi.system.injection.InjectionContainer;

@RequiredArgsConstructor
public class ExtensionLoader {
  private static final String EXTENSIONS_EXCLUDED_DELIMITER = ",";

  @Setting private static final String SETTING_EDC_EXTENSIONS_EXCLUDED = "edc.extensions.excluded";

  @NonNull private final ServiceLocator serviceLocator;

  /** Loads and orders the service extensions. */
  public List<InjectionContainer<ServiceExtension>> loadServiceExtensions(
      @NonNull final Config config) {
    final List<ServiceExtension> serviceExtensions = loadExtensions(ServiceExtension.class, true);

    final List<String> filteredExtensionNames = getFilteredExtensionNames(config);
    final List<ServiceExtension> filteredExtensions =
        filterExtensions(serviceExtensions, filteredExtensionNames);

    return new DependencyGraph().of(filteredExtensions);
  }

  private List<ServiceExtension> filterExtensions(
      @NonNull final List<ServiceExtension> serviceExtensions,
      @NonNull final List<String> filteredExtensionNames) {
    if (serviceExtensions.isEmpty() || filteredExtensionNames.isEmpty()) {
      return serviceExtensions;
    }

    return serviceExtensions.stream()
        .filter(Objects::nonNull)
        .filter(serviceExtension -> !isFiltered(serviceExtension, filteredExtensionNames))
        .collect(Collectors.toList());
  }

  private List<String> getFilteredExtensionNames(@NonNull final Config config) {
    final String excludedExtensionNames = config.getString(SETTING_EDC_EXTENSIONS_EXCLUDED);

    final String[] excludedExtensionNamesArray =
        excludedExtensionNames.split(EXTENSIONS_EXCLUDED_DELIMITER);

    return Arrays.stream(excludedExtensionNamesArray)
        .map(String::trim)
        .filter(Predicate.not(String::isBlank))
        .distinct()
        .collect(Collectors.toList());
  }

  private boolean isFiltered(
      final ServiceExtension serviceExtension, List<String> extensionsToFilter) {
    final String className = serviceExtension.getClass().getName();

    return extensionsToFilter.stream().noneMatch(className::equals);
  }

  /** Loads multiple extensions, raising an exception if at least one is not found. */
  public <T> List<T> loadExtensions(@NonNull final Class<T> type, final boolean required) {
    return Optional.ofNullable(serviceLocator.loadImplementors(type, required))
        .orElseGet(Collections::emptyList);
  }
}
