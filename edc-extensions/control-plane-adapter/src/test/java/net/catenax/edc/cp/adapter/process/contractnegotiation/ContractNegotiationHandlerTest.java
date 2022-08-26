package net.catenax.edc.cp.adapter.process.contractnegotiation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.catenax.edc.cp.adapter.dto.ProcessData;
import net.catenax.edc.cp.adapter.messaging.Message;
import net.catenax.edc.cp.adapter.messaging.MessageService;
import net.catenax.edc.cp.adapter.process.contractdatastore.ContractAgreementData;
import net.catenax.edc.cp.adapter.process.contractdatastore.ContractDataStore;
import org.eclipse.dataspaceconnector.api.datamanagement.catalog.service.CatalogService;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.service.ContractNegotiationService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ContractNegotiationHandlerTest {
  @Mock Monitor monitor;
  @Mock MessageService messageService;
  @Mock ContractNegotiationService contractNegotiationService;
  @Mock CatalogService catalogService;
  @Mock ContractDataStore contractDataStore;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void process_shouldNotInitializeContractNegotiationWhenCachedContractAlreadyAvailable() {
    // given
    ContractNegotiationHandler contractNegotiationHandler =
        new ContractNegotiationHandler(
            monitor, messageService, contractNegotiationService, catalogService, contractDataStore);

    when(contractDataStore.get(anyString(), anyString()))
        .thenReturn(getValidContractAgreementData());

    // when
    contractNegotiationHandler.process(new Message(new ProcessData("asset", "provider")));

    // then
    verify(contractNegotiationService, times(0)).initiateNegotiation(any());
    verify(messageService, times(1)).send(any(), any(Message.class));
  }

  @Test
  public void process_shouldInitializeContractNegotiationWhenCachedContractExpired() {
    // given
    ContractNegotiationHandler contractNegotiationHandler =
        new ContractNegotiationHandler(
            monitor, messageService, contractNegotiationService, catalogService, contractDataStore);

    when(contractDataStore.get(anyString(), anyString()))
        .thenReturn(getExpiredContractAgreementData());
    when(catalogService.getByProviderUrl(anyString()))
        .thenReturn(CompletableFuture.completedFuture(getCatalog()));
    when(contractNegotiationService.initiateNegotiation(any()))
        .thenReturn(getContractNegotiation());

    // when
    contractNegotiationHandler.process(new Message(new ProcessData("assetId", "provider")));

    // then
    verify(contractNegotiationService, times(1)).initiateNegotiation(any());
    verify(messageService, times(1)).send(any(), any(Message.class));
  }

  @Test
  public void process_shouldInitiateContractNegotiationAndSendMessageFurtherIfCacheEmpty() {
    // given
    ContractNegotiationHandler contractNegotiationHandler = new ContractNegotiationHandler(
            monitor, messageService, contractNegotiationService, catalogService, contractDataStore);

    when(contractDataStore.get(anyString(), anyString())).thenReturn(null);
    when(catalogService.getByProviderUrl(anyString()))
        .thenReturn(CompletableFuture.completedFuture(getCatalog()));
    when(contractNegotiationService.initiateNegotiation(any()))
        .thenReturn(getContractNegotiation());

    // when
    contractNegotiationHandler.process(new Message(new ProcessData("assetId", "provider")));

    // then
    verify(contractNegotiationService, times(1)).initiateNegotiation(any());
    verify(messageService, times(1)).send(any(), any(Message.class));
  }

  private ContractNegotiation getContractNegotiation() {
    return ContractNegotiation.Builder.newInstance()
        .id("contractNegotiationId")
        .counterPartyId("counterPartyId")
        .counterPartyAddress("counterPartyAddress")
        .protocol("protocol")
        .build();
  }

  private Catalog getCatalog() {
    return Catalog.Builder.newInstance()
        .id("id")
        .contractOffers(List.of(getContractOffer()))
        .build();
  }

  private ContractOffer getContractOffer() {
    Asset asset = Asset.Builder.newInstance().id("assetId").build();
    return ContractOffer.Builder.newInstance().id("id").asset(asset).policyId("policyId").build();
  }

  private ContractAgreementData getValidContractAgreementData() {
    long now = Instant.now().getEpochSecond();
    ContractAgreementData contractAgreementData = new ContractAgreementData();
    contractAgreementData.setId("id");
    contractAgreementData.setAssetId("assetId");
    contractAgreementData.setContractStartDate(now - 5000);
    contractAgreementData.setContractEndDate(now + 5000);
    return contractAgreementData;
  }

  private ContractAgreementData getExpiredContractAgreementData() {
    long now = Instant.now().getEpochSecond();
    ContractAgreementData contractAgreementData = getValidContractAgreementData();
    contractAgreementData.setContractEndDate(now - 1000);
    return contractAgreementData;
  }
}
