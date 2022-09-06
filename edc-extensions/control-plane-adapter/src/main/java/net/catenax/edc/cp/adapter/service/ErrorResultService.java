package net.catenax.edc.cp.adapter.service;

import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import net.catenax.edc.cp.adapter.exception.ExternalRequestException;
import net.catenax.edc.cp.adapter.exception.ResourceNotFoundException;
import net.catenax.edc.cp.adapter.messaging.Channel;
import net.catenax.edc.cp.adapter.messaging.Listener;
import net.catenax.edc.cp.adapter.messaging.Message;
import net.catenax.edc.cp.adapter.messaging.MessageService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

@RequiredArgsConstructor
public class ErrorResultService implements Listener {
  private static final Map<Class<?>, Response.Status> statusOfException = new HashMap<>();

  static {
    statusOfException.put(ExternalRequestException.class, Response.Status.BAD_GATEWAY);
    statusOfException.put(ResourceNotFoundException.class, Response.Status.NOT_FOUND);
  }

  private final Monitor monitor;
  private final MessageService messageService;

  @Override
  public void process(Message message) {
    message.getPayload().setErrorMessage(getErrorMessage(message));
    message.getPayload().setErrorStatus(statusOfException.getOrDefault(
            message.getFinalException().getClass(),
            Response.Status.INTERNAL_SERVER_ERROR));
    log(message);
    messageService.send(Channel.RESULT, message);
  }

  private String getErrorMessage(Message message) {
    return Objects.nonNull(message.getFinalException())
        ? message.getFinalException().getMessage()
        : "Unrecognized Exception.";
  }

  private void log(Message message) {
    monitor.info(String.format("[%s] Sending ERROR message to RESULT channel: %s / %s ",
            message.getTraceId(),
            message.getPayload().getErrorMessage(),
            message.getPayload().getErrorStatus()));
  }
}
