package net.catenax.edc.cp.adapter.process.contractconfirmation;

import java.util.HashMap;
import java.util.Map;

import net.catenax.edc.cp.adapter.messaging.Message;

public class InMemoryDataStore implements DataStore {
  private final Map<String, Message> messages = new HashMap<>();
  private final Map<String, String> confirmedContracts = new HashMap<>();
  private final DataStoreLock lock;

  public InMemoryDataStore(DataStoreLock lock) {
    this.lock = lock;
  }

  @Override
  public void storeConfirmedContract(String contractNegotiationId, String contractAgreementId) {
    lock.writeLock(contractNegotiationId);
    confirmedContracts.put(contractNegotiationId, contractAgreementId);
    lock.writeUnlock(contractNegotiationId);
  }

  @Override
  public String getConfirmedContract(String contractNegotiationId) {
    lock.readLock(contractNegotiationId);
    String confirmedContract = confirmedContracts.get(contractNegotiationId);
    lock.readUnlock(contractNegotiationId);
    return confirmedContract;
  }

  @Override
  public void removeConfirmedContract(String contractNegotiationId) {
    confirmedContracts.remove(contractNegotiationId);
    lock.removeLock(contractNegotiationId);
  }

  @Override
  public void storeMessage(Message message) {
    String contractNegotiationId = message.getPayload().getContractNegotiationId();

    lock.writeLock(contractNegotiationId);
    messages.put(contractNegotiationId, message);
    lock.writeUnlock(contractNegotiationId);
  }

  @Override
  public Message getMessage(String contractNegotiationId) {
    lock.readLock(contractNegotiationId);
    Message message = messages.get(contractNegotiationId);
    lock.readUnlock(contractNegotiationId);
    return message;
  }

  @Override
  public void removeMessage(String contractNegotiationId) {
    messages.remove(contractNegotiationId);
    lock.removeLock(contractNegotiationId);
  }
}
