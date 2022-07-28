package net.catenax.edc.cp.adapter.process.contractconfirmation;

import net.catenax.edc.cp.adapter.messaging.Message;

public interface DataStore {
  void storeConfirmedContract(String contractNegotiationId, String contractAgreementId);

  String getConfirmedContract(String contractNegotiationId);

  void removeConfirmedContract(String key);

  void storeMessage(Message message);

  Message getMessage(String key);

  void removeMessage(String key);
}
