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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;

public abstract class Message<T> {
  @Getter private final String traceId;
  @Getter private final T payload;
  private final AtomicInteger errorNumber = new AtomicInteger();
  private final int retryLimit;
  @Getter private Exception finalException;

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
    errorNumber.incrementAndGet();
    return getDelayTime();
  }

  protected void clearErrors() {
    errorNumber.set(0);
  }

  protected boolean canRetry() {
    return errorNumber.get() < retryLimit;
  }

  protected void setFinalException(Exception e) {
    this.finalException = e;
  }

  private int getDelayTime() {
    return errorNumber.get() < 5
        ? errorNumber.get() * 750
        : (int) Math.pow(errorNumber.get(), 2) * 150;
  }
}
