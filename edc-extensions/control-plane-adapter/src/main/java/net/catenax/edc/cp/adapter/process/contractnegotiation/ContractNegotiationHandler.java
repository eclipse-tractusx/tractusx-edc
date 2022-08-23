package net.catenax.edc.cp.adapter.process.contractnegotiation;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import net.catenax.edc.cp.adapter.dto.ProcessData;
import net.catenax.edc.cp.adapter.exception.ContractOfferNotAvailable;
import net.catenax.edc.cp.adapter.exception.ResourceNotFoundException;
import net.catenax.edc.cp.adapter.messaging.Channel;
import net.catenax.edc.cp.adapter.messaging.Listener;
import net.catenax.edc.cp.adapter.messaging.Message;
import net.catenax.edc.cp.adapter.messaging.MessageService;
import net.catenax.edc.cp.adapter.process.contractdatastore.ContractAgreementData;
import net.catenax.edc.cp.adapter.process.contractdatastore.ContractDataStore;
import org.eclipse.dataspaceconnector.api.datamanagement.catalog.service.CatalogService;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.service.ContractNegotiationService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

@RequiredArgsConstructor
public class ContractNegotiationHandler implements Listener {
  private final Monitor monitor;
  private final MessageService messageService;
  private final ContractNegotiationService contractNegotiationService;
  private final CatalogService catalogService;
  private final ContractDataStore contractDataStore;

  @Override
  public void process(Message message) {
    monitor.debug("RequestHandler: input request: " + message.getPayload());
    ProcessData processData = message.getPayload();

    ContractAgreementData contractData = contractDataStore.get(
            message.getPayload().getAssetId(),
            message.getPayload().getProvider());
    if (Objects.nonNull(contractData) && isContractValid(contractData)) {
      monitor.info(String.format("[%s] ContractAgreement taken from cache.", message.getTraceId()));
      message.getPayload().setContractAgreementId(contractData.getId());
      message.getPayload().setContractConfirmed(true);
      messageService.send(Channel.CONTRACT_CONFIRMATION, message);
      return;
    }

    ContractOffer contractOffer =
        findContractOffer(processData.getAssetId(), processData.getProvider());

    String contractNegotiationId =
        initializeContractNegotiation(
            contractOffer, message.getPayload().getProvider(), message.getTraceId());
    message.getPayload().setContractNegotiationId(contractNegotiationId);

    messageService.send(Channel.CONTRACT_CONFIRMATION, message);
  }

  private boolean isContractValid(ContractAgreementData contractAgreement) {
    long now = Instant.now().getEpochSecond();
    return Objects.nonNull(contractAgreement) &&
            contractAgreement.getContractStartDate() < now &&
            contractAgreement.getContractEndDate() > now;
  }

  private ContractOffer findContractOffer(String assetId, String providerUrl) {
    Catalog catalog = getCatalog(providerUrl);
    return Optional.ofNullable(catalog.getContractOffers()).orElse(Collections.emptyList()).stream()
            .filter(it -> it.getAsset().getId().equals(assetId))
            .findFirst()
            .orElseThrow(
                    () ->
                            new ResourceNotFoundException("Could not find Contract Offer for given Asset Id"));
  }

  private Catalog getCatalog(String providerUrl) {
    try {
      return catalogService.getByProviderUrl(providerUrl).get();
    } catch (InterruptedException | ExecutionException e) {
      throw new ContractOfferNotAvailable(e);
    }
  }

  private String initializeContractNegotiation(
      ContractOffer contractOffer, String providerUrl, String traceId) {
    monitor.info(String.format("[%s] RequestHandler: initiateNegotiation - start", traceId));
    ContractOfferRequest contractOfferRequest =
        ContractOfferRequest.Builder.newInstance()
            .connectorAddress(providerUrl)
            .contractOffer(contractOffer)
            .type(ContractOfferRequest.Type.INITIAL)
            .connectorId("provider")
            .protocol("ids-multipart")
            .correlationId(traceId)
            .build();

    ContractNegotiation contractNegotiation =
        contractNegotiationService.initiateNegotiation(contractOfferRequest);
    monitor.info(String.format("[%s] RequestHandler: initiateNegotiation - end", traceId));
    return Optional.ofNullable(contractNegotiation.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Could not find Contract NegotiationId"));
  }
}
