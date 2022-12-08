package net.catenax.edc.cp.adapter.process.datareference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.catenax.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import net.catenax.edc.cp.adapter.dto.ProcessData;
import net.catenax.edc.cp.adapter.messaging.Channel;
import net.catenax.edc.cp.adapter.messaging.Message;
import net.catenax.edc.cp.adapter.messaging.MessageBus;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceReceiver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class EndpointDataReferenceReceiverTest {
  @Mock Monitor monitor;
  @Mock MessageBus messageBus;
  @Mock DataRefNotificationSyncService syncService;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void send_shouldNotSendResultWhenMessageNotAvailable() {
    // given
    EndpointDataReferenceReceiver referenceReceiver =
        new EndpointDataReferenceReceiverImpl(monitor, messageBus, syncService);

    // when
    referenceReceiver.send(getEndpointDataReference());

    // then
    verify(messageBus, times(0)).send(eq(Channel.RESULT), any(Message.class));
  }

  @Test
  public void send_shouldSendResultWhenMessageIsAvailable() {
    // given
    DataReferenceRetrievalDto dto = new DataReferenceRetrievalDto(getProcessData(), 3);
    when(syncService.exchangeDataReference(any(), any())).thenReturn(dto);
    EndpointDataReferenceReceiver referenceReceiver =
        new EndpointDataReferenceReceiverImpl(monitor, messageBus, syncService);

    // when
    referenceReceiver.send(getEndpointDataReference());

    // then
    verify(messageBus, times(1)).send(eq(Channel.RESULT), any(Message.class));
    verify(syncService, times(1)).removeDto(any());
  }

  private EndpointDataReference getEndpointDataReference() {
    return EndpointDataReference.Builder.newInstance()
        .endpoint("endpoint")
        .authCode("authCode")
        .authKey("authKey")
        .build();
  }

  private ProcessData getProcessData() {
    return ProcessData.builder().assetId("assetId").provider("provider").build();
  }
}
