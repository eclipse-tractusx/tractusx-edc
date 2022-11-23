package net.catenax.edc.cp.adapter.process.datareference;

import static java.util.Objects.isNull;

import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import net.catenax.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import net.catenax.edc.cp.adapter.messaging.Channel;
import net.catenax.edc.cp.adapter.messaging.MessageBus;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiver;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class EndpointDataReferenceReceiverImpl implements EndpointDataReferenceReceiver {
  private final Monitor monitor;
  private final MessageBus messageBus;
  private final DataRefNotificationSyncService syncService;

  @Override
  public CompletableFuture<Result<Void>> send(@NotNull EndpointDataReference dataReference) {
    String contractAgreementId = dataReference.getProperties().get("cid");
    monitor.info(String.format("DataReference received, contractAgr.: %s", contractAgreementId));

    DataReferenceRetrievalDto dto =
        syncService.exchangeDataReference(dataReference, contractAgreementId);
    if (isNull(dto)) {
      return CompletableFuture.completedFuture(Result.success());
    }
    dto.getPayload().setEndpointDataReference(dataReference);
    messageBus.send(Channel.RESULT, dto);
    syncService.removeDto(contractAgreementId);

    monitor.info(String.format("[%s] DataReference processed.", dto.getTraceId()));
    return CompletableFuture.completedFuture(Result.success());
  }
}
