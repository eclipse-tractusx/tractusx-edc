package net.catenax.edc.cp.adapter.service;

import static java.util.Objects.isNull;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import net.catenax.edc.cp.adapter.dto.ProcessData;
import net.catenax.edc.cp.adapter.messaging.Listener;
import net.catenax.edc.cp.adapter.messaging.Message;

@RequiredArgsConstructor
public class ResultService implements Listener {
  private final int CAPACITY = 1;
  private final int DEFAULT_TIMEOUT = 15; // TODO move to config
  private final Map<String, ArrayBlockingQueue<ProcessData>> results = new ConcurrentHashMap<>();

  public ProcessData pull(String id) throws InterruptedException {
    return pull(id, DEFAULT_TIMEOUT, SECONDS);
  }

  public ProcessData pull(String id, long timeout, TimeUnit unit) throws InterruptedException {
    if (!results.containsKey(id)) {
      initiate(id);
    }
    ProcessData result = results.get(id).poll(timeout, unit);
    results.remove(id);
    return result;
  }

  @Override
  public void process(Message message) {
    if (isNull(message) || isNull(message.getPayload())) {
      throw new IllegalArgumentException();
    }
    add(message.getTraceId(), message.getPayload());
  }

  private void add(String id, ProcessData ProcessData) {
    if (!results.containsKey(id)) {
      initiate(id);
    }
    results.get(id).add(ProcessData);
  }

  private void initiate(String id) {
    results.put(id, new ArrayBlockingQueue<>(CAPACITY));
  }
}
