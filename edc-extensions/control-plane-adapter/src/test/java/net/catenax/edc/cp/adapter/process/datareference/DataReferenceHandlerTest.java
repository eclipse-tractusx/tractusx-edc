package net.catenax.edc.cp.adapter.process.datareference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import net.catenax.edc.cp.adapter.dto.ProcessData;
import net.catenax.edc.cp.adapter.messaging.Channel;
import net.catenax.edc.cp.adapter.messaging.Message;
import net.catenax.edc.cp.adapter.messaging.MessageService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DataReferenceHandlerTest {
  @Mock Monitor monitor;
  @Mock MessageService messageService;
  @Mock DataStore dataStore;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void process_shouldSaveMessageWhenDataReferenceNotAvailable() {
    // given
    DataReferenceHandler dataReferenceHandler =
        new DataReferenceHandler(monitor, messageService, dataStore);
    Message message = new Message(new ProcessData("assetId", "providerUrl"));

    // when
    dataReferenceHandler.process(message);

    // then
    verify(dataStore, times(1)).storeMessage(eq(message));
    verify(messageService, times(0)).send(eq(Channel.RESULT), any(Message.class));
  }

  @Test
  public void process_shouldSendResultWhenDataReferenceIsAvailable() {
    // given
    when(dataStore.getDataReference(any())).thenReturn(getEndpointDataReference());
    DataReferenceHandler dataReferenceHandler =
        new DataReferenceHandler(monitor, messageService, dataStore);
    Message message = new Message(new ProcessData("assetId", "providerUrl"));

    // when
    dataReferenceHandler.process(message);

    // then
    verify(dataStore, times(0)).storeMessage(eq(message));
    verify(messageService, times(1)).send(eq(Channel.RESULT), any(Message.class));
    verify(dataStore, times(1)).removeDataReference(any());
  }

  @Test
  public void send_shouldSaveDataReferenceWhenMessageNotAvailable() {
    // given
    DataReferenceHandler dataReferenceHandler =
        new DataReferenceHandler(monitor, messageService, dataStore);

    // when
    dataReferenceHandler.send(getEndpointDataReference());

    // then
    verify(dataStore, times(1)).storeDataReference(any(), any(EndpointDataReference.class));
    verify(messageService, times(0)).send(eq(Channel.RESULT), any(Message.class));
  }

  @Test
  public void send_shouldSendResultWhenMessageIsAvailable() {
    // given
    Message message = new Message(new ProcessData("assetId", "providerUrl"));
    when(dataStore.getMessage(any())).thenReturn(message);
    DataReferenceHandler dataReferenceHandler =
        new DataReferenceHandler(monitor, messageService, dataStore);

    // when
    dataReferenceHandler.send(getEndpointDataReference());

    // then
    verify(dataStore, times(0)).storeDataReference(any(), any(EndpointDataReference.class));
    verify(messageService, times(1)).send(eq(Channel.RESULT), any(Message.class));
    verify(dataStore, times(1)).removeMessage(any());
  }

  private EndpointDataReference getEndpointDataReference() {
    return EndpointDataReference.Builder.newInstance()
        .endpoint("endpoint")
        .authCode("authCode")
        .authKey("authKey")
        .build();
  }
}
