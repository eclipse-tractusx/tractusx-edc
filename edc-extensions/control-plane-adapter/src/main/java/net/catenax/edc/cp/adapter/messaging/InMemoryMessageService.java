package net.catenax.edc.cp.adapter.messaging;

import static java.util.Objects.isNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

@RequiredArgsConstructor
public class InMemoryMessageService implements MessageService {
  private static final int THREAD_POOL_SIZE = 10;
  private final ScheduledExecutorService executorService =
      Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
  private final Monitor monitor;
  private final ListenerService listenerService;

  @Override
  public void send(Channel name, Message message) {
    if (isNull(message)) {
      monitor.warning(String.format("Message is empty, channel: %s", name));
    } else {
      monitor.info(String.format("[%s] Message sent to channel: %s", message.getTraceId(), name));
      executorService.submit(() -> run(name, message));
    }
  }

  /** Returns 'false' if message processing should be retried. * */
  protected boolean run(Channel name, Message message) {
    try {
      listenerService.getListener(name).process(message);
      message.succeeded();
      return true;
    } catch (Exception e) {
      monitor.warning(String.format("[%s] Message processing error.", message.getTraceId()), e);
      if (!message.canRetry()) {
        monitor.warning(String.format("[%s] Message reached retry limit!", message.getTraceId()));
        // TODO move to DLQ
        return true;
      }
      long delayTime = message.unsucceeded();
      executorService.schedule(() -> run(name, message), delayTime, TimeUnit.MILLISECONDS);
      return false;
    }
  }
}
