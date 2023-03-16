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

package org.eclipse.tractusx.edc.cp.adapter.messaging;

import static java.util.Objects.isNull;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.edc.cp.adapter.exception.ConfigurationException;

@RequiredArgsConstructor
public class ListenerService {
  /** only single listener for a message at the moment * */
  private final Map<Channel, Listener> listeners = new HashMap<>();

  public void addListener(Channel name, Listener listener) {
    listeners.put(name, listener);
  }

  public void removeListener(Channel name) {
    listeners.remove(name);
  }

  Listener getListener(Channel name) {
    Listener listener = listeners.get(name);
    if (isNull(listener)) {
      throw new ConfigurationException("No listener found for channel: " + name);
    }
    return listener;
  }
}
