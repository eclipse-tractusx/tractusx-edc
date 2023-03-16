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

import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public abstract class Message<T> {
  private String traceId;
  private T payload;
  private int errorNumber;
  private int retryLimit;
  private Exception exception;
  private Exception finalException;

  public Message(String traceId, T payload, int retryLimit) {
    this.traceId = traceId;
    this.retryLimit = retryLimit;
    this.payload = payload;
  }

  public Message(T payload, int retryLimit) {
    this.traceId = UUID.randomUUID().toString();
    this.retryLimit = retryLimit;
    this.payload = payload;
  }

  protected long unsucceeded() {
    errorNumber++;
    return getDelayTime();
  }

  protected void clearErrors() {
    errorNumber = 0;
  }

  protected boolean canRetry() {
    return errorNumber < retryLimit;
  }

  protected void setFinalException(Exception e) {
    this.finalException = e;
  }

  private int getDelayTime() {
    return errorNumber < 5 ? errorNumber * 750 : (int) Math.pow(errorNumber, 2) * 150;
  }
}
