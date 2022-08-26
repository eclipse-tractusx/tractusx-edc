package net.catenax.edc.cp.adapter.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class InMemoryMessageServiceTest {
  @Mock Monitor monitor;
  @Mock Listener listener;
  @Mock ListenerService listenerService;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void send_shouldCallListenerOnce() throws InterruptedException {
    // given
    Message message = new Message(null);
    when(listenerService.getListener(any())).thenReturn(listener);
    MessageService messageService = new InMemoryMessageService(monitor, listenerService);

    // when
    messageService.send(Channel.INITIAL, message);

    // then
    Thread.sleep(50);
    verify(listener, times(1)).process(any(Message.class));
  }

  @Test
  public void send_shouldCallListenerWithRetryOnException() throws InterruptedException {
    // given
    Message message = new Message(null);
    when(listenerService.getListener(any())).thenReturn(listener);
    doThrow(new IllegalStateException()).doNothing().when(listener).process(any());
    MessageService messageService = new InMemoryMessageService(monitor, listenerService);

    // when
    messageService.send(Channel.INITIAL, message);

    // then
    Thread.sleep(1000);
    verify(listener, times(2)).process(any(Message.class));
  }
}
