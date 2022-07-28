package net.catenax.edc.cp.adapter;

import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class ApiAdapterConfig {
  private static final String DEFAULT_MESSAGE_RETRY_NUMBER =
      "edc.cp.adapter.default.message.retry.number";

  private final ServiceExtensionContext context;

  public ApiAdapterConfig(ServiceExtensionContext context) {
    this.context = context;
  }

  public String getDefaultMessageRetryNumber() {
    return context.getSetting(DEFAULT_MESSAGE_RETRY_NUMBER, null);
  }
}
