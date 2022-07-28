package net.catenax.edc.cp.adapter.process.contractconfirmation;

import java.util.HashMap;
import java.util.Map;
import net.catenax.edc.cp.adapter.messaging.Message;

public class InMemoryDataStore implements DataStore {
  private final Map<String, Message> messages = new HashMap<>();
  private final Map<String, String> confirmedContracts = new HashMap<>();

  @Override
  public void storeConfirmedContract(String contractNegotiationId, String contractAgreementId) {
    confirmedContracts.put(contractNegotiationId, contractAgreementId);
  }

  @Override
  public String getConfirmedContract(String contractNegotiationId) {
    return confirmedContracts.get(contractNegotiationId);
  }

  @Override
  public void removeConfirmedContract(String contractNegotiationId) {
    confirmedContracts.remove(contractNegotiationId);
  }

  @Override
  public void storeMessage(Message message) {
    messages.put(message.getPayload().getContractNegotiationId(), message);
  }

  @Override
  public Message getMessage(String contractNegotiationId) {
    return messages.get(contractNegotiationId);
  }

  @Override
  public void removeMessage(String contractNegotiationId) {
    messages.remove(contractNegotiationId);
  }
}
