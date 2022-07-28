package net.catenax.edc.cp.adapter.messaging;

public interface MessageService {
  void send(Channel name, Message message);
}
