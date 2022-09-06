package net.catenax.edc.cp.adapter.process.contractnotification;

import static java.util.Objects.isNull;

import java.util.HashMap;
import java.util.Map;
import net.catenax.edc.cp.adapter.messaging.Message;
import net.catenax.edc.cp.adapter.util.LockMap;

public class InMemoryDataStore implements DataStore {
  private final Map<String, Message> messages = new HashMap<>();
  private final Map<String, ContractInfo> contractInfoMap = new HashMap<>();
  private final LockMap locks;

  public InMemoryDataStore(LockMap locks) {
    this.locks = locks;
  }

  @Override
  public Message exchangeConfirmedContract(String negotiationId, String agreementId) {
    locks.lock(negotiationId);
    Message message = messages.get(negotiationId);
    if (isNull(message)) {
      contractInfoMap.put(negotiationId,
          new ContractInfo(agreementId, ContractInfo.ContractState.CONFIRMED));
    }
    locks.unlock(negotiationId);
    return message;
  }

  @Override
  public Message exchangeDeclinedContract(String negotiationId) {
    locks.lock(negotiationId);
    Message message = messages.get(negotiationId);
    if (isNull(message)) {
      contractInfoMap.put(
          negotiationId,
          new ContractInfo(ContractInfo.ContractState.DECLINED));
    }
    locks.unlock(negotiationId);
    return message;
  }

  @Override
  public Message exchangeErrorContract(String negotiationId) {
    locks.lock(negotiationId);
    Message message = messages.get(negotiationId);
    if (isNull(message)) {
      contractInfoMap.put(
          negotiationId,
          new ContractInfo(ContractInfo.ContractState.ERROR));
    }

    locks.unlock(negotiationId);
    return message;
  }

  @Override
  public ContractInfo exchangeMessage(Message message) {
    String negotiationId = message.getPayload().getContractNegotiationId();

    locks.lock(negotiationId);
    ContractInfo contractInfo = contractInfoMap.get(negotiationId);
    if (isNull(contractInfo)) {
      messages.put(negotiationId, message);
    }

    locks.unlock(negotiationId);
    return contractInfo;
  }

  @Override
  public void removeContractInfo(String negotiationId) {
    contractInfoMap.remove(negotiationId);
    locks.removeLock(negotiationId);
  }

  @Override
  public void removeMessage(String negotiationId) {
    messages.remove(negotiationId);
    locks.removeLock(negotiationId);
  }
}
