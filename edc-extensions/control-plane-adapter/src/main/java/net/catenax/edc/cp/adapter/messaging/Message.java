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
import net.catenax.edc.cp.adapter.dto.ProcessData;

public class Message {
  @Getter private final String traceId;
  @Getter private final ProcessData payload;
  private final AtomicInteger errorNumber = new AtomicInteger();
  private int retryLimit = 3; // TODO configure
  @Getter private Exception finalException;

  public Message(String traceId, ProcessData payload, int retryLimit) {
    this(traceId, payload);
    this.retryLimit = retryLimit;
  }

  public Message(String traceId, ProcessData payload) {
    this.payload = payload;
    this.traceId = traceId;
  }

  public Message(ProcessData payload, int retryLimit) {
    this(payload);
    this.retryLimit = retryLimit;
  }

  public Message(ProcessData payload) {
    traceId = UUID.randomUUID().toString();
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
