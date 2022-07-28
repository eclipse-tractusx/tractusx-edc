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

  public Message(String id, ProcessData payload, int retryLimit) {
    this(id, payload);
    this.retryLimit = retryLimit;
  }

  public Message(String id, ProcessData payload) {
    this.payload = payload;
    this.traceId = id;
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

  protected void succeeded() {
    errorNumber.set(0);
  }

  protected boolean canRetry() {
    return errorNumber.get() < retryLimit;
  }

  // TODO external configuration? extract to other class implements BackoffPolicy - strategy
  private int getDelayTime() {
    return errorNumber.get() < 5
        ? errorNumber.get() * 750
        : (int) Math.pow(errorNumber.get(), 2) * 150;
  }
}
