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
