package net.catenax.edc.cp.adapter.process.datareference;

import net.catenax.edc.cp.adapter.messaging.Message;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;

public interface DataStore {
  void storeDataReference(String contractAgreementId, EndpointDataReference endpointDataReference);

  EndpointDataReference getDataReference(String contractAgreementId);

  void removeDataReference(String contractAgreementId);

  void storeMessage(Message message);

  Message getMessage(String key);

  void removeMessage(String key);
}
