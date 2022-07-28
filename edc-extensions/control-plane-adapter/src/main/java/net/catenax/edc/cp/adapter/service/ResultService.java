package net.catenax.edc.cp.adapter.service;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import net.catenax.edc.cp.adapter.messaging.Listener;
import net.catenax.edc.cp.adapter.messaging.Message;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;

@RequiredArgsConstructor
public class ResultService implements Listener {
  private final int CAPACITY = 1;
  private final int DEFAULT_TIMEOUT = 15; // TODO move to config
  private final Monitor monitor;
  private final Map<String, ArrayBlockingQueue<EndpointDataReference>> dataReference =
      new ConcurrentHashMap<>();

  public EndpointDataReference poll(String id) throws InterruptedException {
    return poll(id, DEFAULT_TIMEOUT, SECONDS);
  }

  public EndpointDataReference poll(String id, long timeout, TimeUnit unit)
      throws InterruptedException {
    if (!dataReference.containsKey(id)) {
      initiate(id);
    }
    EndpointDataReference result = dataReference.get(id).poll(timeout, unit);
    dataReference.remove(id);
    return result;
  }

  @Override
  public void process(Message message) {
    add(message.getTraceId(), message.getPayload().getEndpointDataReference());
  }

  private void add(String id, EndpointDataReference dataReferenceDto) {
    if (!dataReference.containsKey(id)) {
      initiate(id);
    }
    dataReference.get(id).add(dataReferenceDto);
  }

  private void initiate(String id) {
    dataReference.put(id, new ArrayBlockingQueue<>(CAPACITY));
  }
}
