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

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.boot.system.ExtensionLoader.loadTelemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.dataspaceconnector.boot.monitor.MonitorProvider;
import org.eclipse.dataspaceconnector.boot.system.DefaultServiceExtensionContext;
import org.eclipse.dataspaceconnector.boot.system.ServiceLocator;
import org.eclipse.dataspaceconnector.boot.system.ServiceLocatorImpl;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ConfigurationExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.configuration.Config;
import org.eclipse.dataspaceconnector.spi.system.configuration.ConfigFactory;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckResult;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;
import org.eclipse.dataspaceconnector.spi.system.injection.InjectionContainer;
import org.eclipse.dataspaceconnector.spi.telemetry.Telemetry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

// Copy & Paste from
// https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/blob/main/core/common/boot/src/main/java/org/eclipse/dataspaceconnector/boot/system/runtime/BaseRuntime.java
// with new ExtensionLoader
public class BootRuntime {

  protected final ServiceLocator serviceLocator;
  private final AtomicReference<HealthCheckResult> startupStatus =
      new AtomicReference<>(HealthCheckResult.failed("Startup not complete"));
  private final ExtensionLoader extensionLoader;
  private final List<ServiceExtension> serviceExtensions = new ArrayList<>();
  protected Monitor monitor;

  public BootRuntime() {
    serviceLocator = new ServiceLocatorImpl();
    extensionLoader = new ExtensionLoader(serviceLocator);
  }

  public static void main(String[] args) {
    BootRuntime runtime = new BootRuntime();
    runtime.boot();
  }

  protected Monitor getMonitor() {
    return monitor;
  }

  /**
   * Main entry point to runtime initialization. Calls all methods and sets up a context shutdown
   * hook at runtime shutdown.
   */
  protected void boot() {
    boot(true);
  }

  /** Main entry point to runtime initialization. Calls all methods. */
  protected void bootWithoutShutdownHook() {
    boot(false);
  }

  @NotNull
  protected ServiceExtensionContext createServiceExtensionContext() {
    var typeManager = createTypeManager();
    monitor = createMonitor();
    MonitorProvider.setInstance(monitor);

    var telemetry = createTelemetry();

    var context = createContext(typeManager, monitor, telemetry);
    initializeContext(context);
    return context;
  }

  @NotNull
  protected Telemetry createTelemetry() {
    return loadTelemetry();
  }

  /**
   * Initializes the context. If {@link BootRuntime#createContext(TypeManager, Monitor, Telemetry)}
   * is overridden and the (custom) context needs to be initialized, this method should be
   * overridden as well.
   *
   * @param context The context.
   */
  protected void initializeContext(ServiceExtensionContext context) {
    context.initialize();
  }

  /**
   * The name of this runtime. This string is solely used for cosmetic/display/logging purposes. By
   * default, {@link ServiceExtensionContext#getConnectorId()} is used.
   */
  protected String getRuntimeName(ServiceExtensionContext context) {
    return context.getConnectorId();
  }

  /** Callback for any error that happened during runtime initialization */
  protected void onError(Exception e) {
    monitor.severe(String.format("Error booting runtime: %s", e.getMessage()), e);
    exit();
  }

  protected void exit() {
    System.exit(-1); // stop the process
  }

  /**
   * Starts all service extensions by invoking {@link
   * org.eclipse.dataspaceconnector.boot.system.ExtensionLoader#bootServiceExtensions(List,
   * ServiceExtensionContext)}
   *
   * @param context The {@code ServiceExtensionContext} that is used in this runtime.
   * @param serviceExtensions a list of extensions
   */
  protected void bootExtensions(
      ServiceExtensionContext context,
      List<InjectionContainer<ServiceExtension>> serviceExtensions) {
    org.eclipse.dataspaceconnector.boot.system.ExtensionLoader.bootServiceExtensions(
        serviceExtensions, context);
  }

  /**
   * Create a list of {@link ServiceExtension}s. By default this is done using the ServiceLoader
   * mechanism. Override if e.g. a custom DI mechanism should be used.
   *
   * @return a list of {@code ServiceExtension}s
   */
  protected List<InjectionContainer<ServiceExtension>> createExtensions() {
    final List<ConfigurationExtension> configurationExtensions = loadConfigurationExtensions();

    final Config config =
        configurationExtensions.stream()
            .map(ConfigurationExtension::getConfig)
            .collect(ConfigFactory::empty, Config::merge, Config::merge);

    return extensionLoader.loadServiceExtensions(config);
  }

  /**
   * Create a {@link ServiceExtensionContext} that will be used in this runtime. If e.g. a
   * third-party dependency-injection framework were to be used, this would likely need to be
   * overridden.
   *
   * @param typeManager The TypeManager (for JSON de-/serialization)
   * @param monitor a Monitor
   * @return a {@code ServiceExtensionContext}
   */
  @NotNull
  protected ServiceExtensionContext createContext(
      TypeManager typeManager, Monitor monitor, Telemetry telemetry) {
    return new DefaultServiceExtensionContext(
        typeManager, monitor, telemetry, loadConfigurationExtensions());
  }

  protected List<ConfigurationExtension> loadConfigurationExtensions() {
    return extensionLoader.loadExtensions(ConfigurationExtension.class, false);
  }

  /**
   * Hook that is called when a runtime is shutdown (e.g. after a CTRL-C command on a command line).
   * It is highly advisable to forward this signal to all extensions through their {@link
   * ServiceExtension#shutdown()} callback.
   */
  protected void shutdown() {
    var iter = serviceExtensions.listIterator(serviceExtensions.size());
    while (iter.hasPrevious()) {
      var extension = iter.previous();
      extension.shutdown();
      monitor.info("Shutdown " + extension.name());
      iter.remove();
    }
    monitor.info("Shutdown complete");
  }

  /**
   * Hook point to instantiate a {@link Monitor}. By default, the runtime instantiates a {@code
   * Monitor} using the Service Loader mechanism, i.e. by calling the {@link
   * org.eclipse.dataspaceconnector.boot.system.ExtensionLoader#loadMonitor()} method.
   *
   * <p>Please consider using the extension mechanism (i.e. {@link
   * org.eclipse.dataspaceconnector.spi.system.MonitorExtension}) rather than supplying a custom
   * monitor by overriding this method. However, for development/testing scenarios it might be an
   * easy solution to just override this method.
   */
  @NotNull
  protected Monitor createMonitor() {
    return org.eclipse.dataspaceconnector.boot.system.ExtensionLoader.loadMonitor();
  }

  /** Hook point to supply a (custom) TypeManager. By default a new TypeManager is created */
  @NotNull
  protected TypeManager createTypeManager() {
    return new TypeManager();
  }

  private void boot(boolean addShutdownHook) {
    ServiceExtensionContext context = createServiceExtensionContext();

    var name = getRuntimeName(context);
    try {
      List<InjectionContainer<ServiceExtension>> newExtensions = createExtensions();
      bootExtensions(context, newExtensions);

      newExtensions.stream()
          .map(InjectionContainer::getInjectionTarget)
          .forEach(serviceExtensions::add);
      if (addShutdownHook) {
        getRuntime().addShutdownHook(new Thread(this::shutdown));
      }

      var healthCheckService = context.getService(HealthCheckService.class);
      healthCheckService.addStartupStatusProvider(this::getStartupStatus);

      startupStatus.set(HealthCheckResult.success());

      healthCheckService.refresh();
    } catch (Exception e) {
      onError(e);
    }

    monitor.info(format("%s ready", name));
  }

  private HealthCheckResult getStartupStatus() {
    return startupStatus.get();
  }
}
