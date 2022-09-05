package net.catenax.edc.cp.adapter.process.contractnotification;

import net.catenax.edc.cp.adapter.messaging.Message;

public interface DataStore {
  void storeConfirmedContract(String contractNegotiationId, String contractAgreementId);

  void storeDeclinedContract(String contractNegotiationId);

  void storeErrorContract(String contractNegotiationId);

  ContractInfo getContractInfo(String contractNegotiationId);

  void removeConfirmedContract(String key);

  void storeMessage(Message message);

  Message getMessage(String key);

  void removeMessage(String key);
}
