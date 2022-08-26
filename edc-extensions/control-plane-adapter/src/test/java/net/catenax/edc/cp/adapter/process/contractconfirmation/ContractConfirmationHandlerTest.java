package net.catenax.edc.cp.adapter.process.contractconfirmation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import net.catenax.edc.cp.adapter.dto.ProcessData;
import net.catenax.edc.cp.adapter.messaging.Channel;
import net.catenax.edc.cp.adapter.messaging.Message;
import net.catenax.edc.cp.adapter.messaging.MessageService;
import net.catenax.edc.cp.adapter.process.contractdatastore.ContractDataStore;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.service.ContractNegotiationService;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.service.TransferProcessService;
import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ContractConfirmationHandlerTest {
  @Mock Monitor monitor;
  @Mock MessageService messageService;
  @Mock ContractNegotiationService contractNegotiationService;
  @Mock DataStore dataStore;
  @Mock ContractDataStore contractDataStore;
  @Mock TransferProcessService transferProcessService;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void process_shouldSaveMessageWhenContractNotConfirmed() {
    // given
    when(transferProcessService.initiateTransfer(any())).thenReturn(ServiceResult.success(null));
    ContractConfirmationHandler contractConfirmationHandler =
        new ContractConfirmationHandler(
            monitor,
            messageService,
            dataStore,
            contractNegotiationService,
            transferProcessService,
            contractDataStore);
    Message message = new Message(new ProcessData("assetId", "providerUrl"));

    // when
    contractConfirmationHandler.process(message);

    // then
    verify(dataStore, times(1)).storeMessage(any());
    verify(transferProcessService, times(0)).initiateTransfer(any());
    verify(messageService, times(0)).send(eq(Channel.DATA_REFERENCE), any(Message.class));
  }

  @Test
  public void process_shouldInitiateTransferWhenContractConfirmedFromCache() {
    // given
    when(transferProcessService.initiateTransfer(any())).thenReturn(ServiceResult.success(null));
    ContractConfirmationHandler contractConfirmationHandler =
        new ContractConfirmationHandler(
            monitor,
            messageService,
            dataStore,
            contractNegotiationService,
            transferProcessService,
            contractDataStore);
    Message message = new Message(new ProcessData("assetId", "providerUrl"));
    message.getPayload().setContractConfirmed(true);

    // when
    contractConfirmationHandler.process(message);

    // then
    verify(transferProcessService, times(1)).initiateTransfer(any());
    verify(messageService, times(1)).send(eq(Channel.DATA_REFERENCE), any(Message.class));
  }

  @Test
  public void process_shouldInitiateTransferWhenAlreadyContractConfirmedAtProvider() {
    // given
    when(contractNegotiationService.findbyId(any())).thenReturn(getConfirmedContractNegotiation());
    when(transferProcessService.initiateTransfer(any())).thenReturn(ServiceResult.success(null));
    ContractConfirmationHandler contractConfirmationHandler =
        new ContractConfirmationHandler(
            monitor,
            messageService,
            dataStore,
            contractNegotiationService,
            transferProcessService,
            contractDataStore);
    Message message = new Message(new ProcessData("assetId", "providerUrl"));

    // when
    contractConfirmationHandler.process(message);

    // then
    verify(dataStore, times(0)).storeMessage(any());
    verify(transferProcessService, times(1)).initiateTransfer(any());
    verify(messageService, times(1)).send(eq(Channel.DATA_REFERENCE), any(Message.class));
  }

  @Test
  public void process_shouldInitiateTransferWhenContractConfirmedByNotification() {
    // given
    when(dataStore.getConfirmedContract(any())).thenReturn("confirmedContractAgreementId");
    when(transferProcessService.initiateTransfer(any())).thenReturn(ServiceResult.success(null));
    ContractConfirmationHandler contractConfirmationHandler =
        new ContractConfirmationHandler(
            monitor,
            messageService,
            dataStore,
            contractNegotiationService,
            transferProcessService,
            contractDataStore);
    Message message = new Message(new ProcessData("assetId", "providerUrl"));

    // when
    contractConfirmationHandler.process(message);

    // then
    verify(dataStore, times(0)).storeMessage(any());
    verify(transferProcessService, times(1)).initiateTransfer(any());
    verify(messageService, times(1)).send(eq(Channel.DATA_REFERENCE), any(Message.class));
    verify(dataStore, times(1)).removeConfirmedContract(any());
  }

  @Test
  public void preConfirmed_shouldSaveInfoAboutContractConfirmationIfMessageNotAvailable() {
    // given
    ContractConfirmationHandler contractConfirmationHandler =
        new ContractConfirmationHandler(
            monitor,
            messageService,
            dataStore,
            contractNegotiationService,
            transferProcessService,
            contractDataStore);
    ContractNegotiation contractNegotiation = getConfirmedContractNegotiation();

    // when
    contractConfirmationHandler.preConfirmed(contractNegotiation);

    // then
    verify(dataStore, times(1)).storeConfirmedContract(any(), any());
    verify(transferProcessService, times(0)).initiateTransfer(any());
    verify(messageService, times(0)).send(eq(Channel.DATA_REFERENCE), any(Message.class));
  }

  @Test
  public void preConfirmed_shouldInitiateTransferIfMessageIsAvailable() {
    // given
    when(dataStore.getMessage(any()))
        .thenReturn(new Message(new ProcessData("assetId", "providerUrl")));
    when(transferProcessService.initiateTransfer(any())).thenReturn(ServiceResult.success(null));
    ContractConfirmationHandler contractConfirmationHandler =
        new ContractConfirmationHandler(
            monitor,
            messageService,
            dataStore,
            contractNegotiationService,
            transferProcessService,
            contractDataStore);
    ContractNegotiation contractNegotiation = getConfirmedContractNegotiation();

    // when
    contractConfirmationHandler.preConfirmed(contractNegotiation);

    // then
    verify(dataStore, times(0)).storeConfirmedContract(any(), any());
    verify(transferProcessService, times(1)).initiateTransfer(any());
    verify(messageService, times(1)).send(eq(Channel.DATA_REFERENCE), any(Message.class));
  }

  private ContractNegotiation getConfirmedContractNegotiation() {
    return ContractNegotiation.Builder.newInstance()
        .state(ContractNegotiationStates.CONFIRMED.code())
        .id("contractNegotiationId")
        .counterPartyId("counterPartyId")
        .counterPartyAddress("counterPartyAddress")
        .protocol("protocol")
        .contractAgreement(
            ContractAgreement.Builder.newInstance()
                .id("contractAgreementId")
                .providerAgentId("providerAgentId")
                .consumerAgentId("consumerAgentId")
                .assetId("assetId")
                .policyId("policyId")
                .build())
        .build();
  }
}
