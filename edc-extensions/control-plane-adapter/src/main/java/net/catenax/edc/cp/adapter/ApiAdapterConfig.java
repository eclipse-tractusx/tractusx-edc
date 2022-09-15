/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 * ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

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
