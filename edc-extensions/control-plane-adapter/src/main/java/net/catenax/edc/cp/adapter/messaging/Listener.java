package net.catenax.edc.cp.adapter.messaging;

public interface Listener {
  void process(Message message);
}
