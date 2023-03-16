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

package org.eclipse.tractusx.edc.cp.adapter.store.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Message;

@Getter
@Setter
@Builder
public class QueueMessage {
  private String id;
  private long createdAt;
  private String channel;
  private Message<?> message;
  private long invokeAfter;
}
