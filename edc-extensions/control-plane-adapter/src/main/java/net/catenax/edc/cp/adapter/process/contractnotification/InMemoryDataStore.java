package net.catenax.edc.cp.adapter.process.contractnotification;

import java.util.HashMap;
import java.util.Map;
import net.catenax.edc.cp.adapter.messaging.Message;

public class InMemoryDataStore implements DataStore {
  private final Map<String, Message> messages = new HashMap<>();
  private final Map<String, ContractInfo> confirmedContracts = new HashMap<>();
  private final DataStoreLock lock;

  public InMemoryDataStore(DataStoreLock lock) {
    this.lock = lock;
  }

  @Override
  public void storeConfirmedContract(String contractNegotiationId, String contractAgreementId) {
    lock.writeLock(contractNegotiationId);
    confirmedContracts.put(contractNegotiationId, new ContractInfo(contractAgreementId, ContractInfo.ContractState.CONFIRMED));
    lock.writeUnlock(contractNegotiationId);
  }

  @Override
  public void storeDeclinedContract(String contractNegotiationId) {
    lock.writeLock(contractNegotiationId);
    confirmedContracts.put(contractNegotiationId, new ContractInfo(ContractInfo.ContractState.DECLINED));
    lock.writeUnlock(contractNegotiationId);
  }

  @Override
  public void storeErrorContract(String contractNegotiationId) {
    lock.writeLock(contractNegotiationId);
    confirmedContracts.put(contractNegotiationId, new ContractInfo(ContractInfo.ContractState.ERROR));
    lock.writeUnlock(contractNegotiationId);
  }

  @Override
  public ContractInfo getContractInfo(String contractNegotiationId) {
    lock.readLock(contractNegotiationId);
    ContractInfo contractInfo = confirmedContracts.get(contractNegotiationId);
    lock.readUnlock(contractNegotiationId);
    return contractInfo;
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
