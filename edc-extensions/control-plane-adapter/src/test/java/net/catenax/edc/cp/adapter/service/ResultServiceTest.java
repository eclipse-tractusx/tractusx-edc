package net.catenax.edc.cp.adapter.service;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.TimeUnit;
import net.catenax.edc.cp.adapter.dto.ProcessData;
import net.catenax.edc.cp.adapter.messaging.Message;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResultServiceTest {
  @Test
  public void pull_shouldReturnDataReferenceWhenMessageOccursFirst() throws InterruptedException {
    // given
    ResultService resultService = new ResultService();
    String endpointDataRefId = "456";
    Message message = getMessage(endpointDataRefId);
    ProcessData processData;

    // when
    resultService.process(message);
    processData = resultService.pull(message.getTraceId(), 200, TimeUnit.MILLISECONDS);

    // then
    Assertions.assertEquals(endpointDataRefId, processData.getEndpointDataReference().getId());
  }

  @Test
  public void pull_shouldReturnDataReferenceWhenMessageOccursSecond() throws InterruptedException {
    // given
    ResultService resultService = new ResultService();
    String endpointDataRefId = "456";
    Message message = getMessage(endpointDataRefId);
    ProcessData processData;

    // when
    processMessageWithDelay(resultService, message);
    processData = resultService.pull(message.getTraceId(), 1000, TimeUnit.MILLISECONDS);

    // then
    Assertions.assertEquals(endpointDataRefId, processData.getEndpointDataReference().getId());
  }

  private void processMessageWithDelay(ResultService resultService, Message message) {
    new Thread(
            () -> {
              sleep(400);
              resultService.process(message);
            })
        .start();
  }

  @Test
  public void pull_shouldReturnNullOnTimeout() throws InterruptedException {
    // given
    ResultService resultService = new ResultService();

    // when
    ProcessData processData = resultService.pull("123", 500, TimeUnit.MILLISECONDS);

    // then
    Assertions.assertNull(processData);
  }

  @Test
  public void process_shouldThrowIllegalArgumentExceptionIfNoDataPayload() {
    // given
    ResultService resultService = new ResultService();
    Message message = new Message(null);

    // when then
    try {
      resultService.process(message);
      fail("Method should throw IllegalArgumentException");
    } catch (IllegalArgumentException ignored) {
    }
  }

  private Message getMessage(String endpointDataRefId) {
    Message message = new Message(new ProcessData("123", "providerUrl"));
    message
        .getPayload()
        .setEndpointDataReference(
            EndpointDataReference.Builder.newInstance()
                .id(endpointDataRefId)
                .endpoint("e")
                .authCode("c")
                .authKey("k")
                .build());
    return message;
  }

  private void sleep(long milisec) {
    try {
      Thread.sleep(milisec);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
