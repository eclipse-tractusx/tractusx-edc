package net.catenax.edc.cp.adapter.process.datareference;

import java.util.HashMap;
import java.util.Map;
import net.catenax.edc.cp.adapter.messaging.Message;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;

public class InMemoryDataStore implements DataStore {
  private final Map<String, Message> messages = new HashMap<>();
  private final Map<String, EndpointDataReference> dataReferences = new HashMap<>();

  @Override
  public void storeDataReference(
      String contractAgreementId, EndpointDataReference endpointDataReference) {
    dataReferences.put(contractAgreementId, endpointDataReference);
  }

  @Override
  public EndpointDataReference getDataReference(String contractAgreementId) {
    return dataReferences.get(contractAgreementId);
  }

  @Override
  public void removeDataReference(String contractAgreementId) {
    dataReferences.remove(contractAgreementId);
  }

  @Override
  public void storeMessage(Message message) {
    messages.put(message.getPayload().getContractAgreementId(), message);
  }

  @Override
  public Message getMessage(String contractAgreementId) {
    return messages.get(contractAgreementId);
  }

  @Override
  public void removeMessage(String contractAgreementId) {
    messages.remove(contractAgreementId);
  }
}
