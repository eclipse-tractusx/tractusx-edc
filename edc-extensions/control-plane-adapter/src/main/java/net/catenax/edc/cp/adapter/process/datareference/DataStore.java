package net.catenax.edc.cp.adapter.process.datareference;

import net.catenax.edc.cp.adapter.messaging.Message;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;

public interface DataStore {
  EndpointDataReference exchangeMessage(Message message, String contractAgreementId);

  Message exchangeDataReference(EndpointDataReference dataReference, String contractAgreementId);

  void removeDataReference(String contractAgreementId);

  void removeMessage(String key);
}
