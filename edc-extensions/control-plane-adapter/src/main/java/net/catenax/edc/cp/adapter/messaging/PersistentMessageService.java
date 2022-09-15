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

package net.catenax.edc.cp.adapter.messaging;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

public class PersistentMessageService extends InMemoryMessageService {
  public PersistentMessageService(Monitor monitor, ListenerService listenerService) {
    super(monitor, listenerService);
    // TODO init scheduler from DB (use send method?)
  }

  @Override
  public void send(Channel name, Message message) {
    // TODO save to db
    super.send(name, message);
  }

  @Override
  protected boolean run(Channel name, Message message) {
    boolean isProcessed = super.run(name, message);
    if (isProcessed) {
      // TODO remove from DB
    }
    return isProcessed;
  }
}
