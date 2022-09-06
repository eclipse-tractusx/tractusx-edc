package net.catenax.edc.cp.adapter.process.contractnotification;

import net.catenax.edc.cp.adapter.messaging.Message;

public interface DataStore {
  Message exchangeConfirmedContract(String contractNegotiationId, String contractAgreementId);

  Message exchangeDeclinedContract(String contractNegotiationId);

  Message exchangeErrorContract(String contractNegotiationId);

  ContractInfo exchangeMessage(Message message);

  void removeContractInfo(String key);

  void removeMessage(String key);
}
