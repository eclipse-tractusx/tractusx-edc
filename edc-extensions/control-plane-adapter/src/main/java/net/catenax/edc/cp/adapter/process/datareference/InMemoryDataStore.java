package net.catenax.edc.cp.adapter.process.datareference;

import static java.util.Objects.isNull;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.catenax.edc.cp.adapter.messaging.Message;
import net.catenax.edc.cp.adapter.util.LockMap;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;

@RequiredArgsConstructor
public class InMemoryDataStore implements DataStore {
  private final Map<String, Message> messages = new HashMap<>();
  private final Map<String, EndpointDataReference> dataReferences = new HashMap<>();
  private final LockMap locks;

  @Override
  public EndpointDataReference exchangeMessage(Message message, String agreementId) {
    locks.lock(agreementId);
    EndpointDataReference dataReference = dataReferences.get(agreementId);
    if (isNull(dataReference)) {
      messages.put(agreementId, message);
    }
    locks.unlock(agreementId);
    return dataReference;
  }

  @Override
  public Message exchangeDataReference(EndpointDataReference dataReference, String agreementId) {
    locks.lock(agreementId);
    Message message = messages.get(agreementId);
    if (isNull(message)) {
      dataReferences.put(agreementId, dataReference);
    }
    locks.unlock(agreementId);
    return message;
  }

  @Override
  public void removeDataReference(String agreementId) {
    dataReferences.remove(agreementId);
    locks.removeLock(agreementId);
  }

  @Override
  public void removeMessage(String agreementId) {
    messages.remove(agreementId);
    locks.removeLock(agreementId);
  }
}
