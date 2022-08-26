package net.catenax.edc.cp.adapter.process.contractconfirmation;

import static java.util.Objects.isNull;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import net.catenax.edc.cp.adapter.exception.DataReferenceAccessException;
import net.catenax.edc.cp.adapter.messaging.Channel;
import net.catenax.edc.cp.adapter.messaging.Listener;
import net.catenax.edc.cp.adapter.messaging.Message;
import net.catenax.edc.cp.adapter.messaging.MessageService;
import net.catenax.edc.cp.adapter.process.contractdatastore.ContractDataStore;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.service.ContractNegotiationService;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.service.TransferProcessService;
import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationListener;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;

@RequiredArgsConstructor
public class ContractConfirmationHandler implements Listener, ContractNegotiationListener {
  private final Monitor monitor;
  private final MessageService messageService;
  private final DataStore dataStore;
  private final ContractNegotiationService contractNegotiationService;
  private final TransferProcessService transferProcessService;
  private final ContractDataStore contractDataStore;

  @Override
  public void process(Message message) {
    monitor.info(
        String.format("[%s] ContractConfirmationHandler: received message.", message.getTraceId()));
    String contractNegotiationId = message.getPayload().getContractNegotiationId();

    if (message.getPayload().isContractConfirmed()) {
      initiateDataTransfer(message);
      return;
    }

    ContractNegotiation contractNegotiation =
        contractNegotiationService.findbyId(contractNegotiationId);
    if (Objects.nonNull(contractNegotiation)
        && contractNegotiation.getState() == ContractNegotiationStates.CONFIRMED.code()) {
      message
          .getPayload()
          .setContractAgreementId(contractNegotiation.getContractAgreement().getId());
      initiateDataTransfer(message);
      return;
    }

    String confirmedContractAgreementId = dataStore.getConfirmedContract(contractNegotiationId);
    if (isNull(confirmedContractAgreementId)) {
      dataStore.storeMessage(message);
      return;
    }

    message.getPayload().setContractAgreementId(confirmedContractAgreementId);
    initiateDataTransfer(message);
    dataStore.removeConfirmedContract(contractNegotiationId);
  }

  @Override
  public void preConfirmed(ContractNegotiation negotiation) {
    monitor.info("ContractConfirmationHandler: received 'ContractNegotiation' event");
    String contractNegotiationId = negotiation.getId();
    String contractAgreementId = negotiation.getContractAgreement().getId();
    Message message = dataStore.getMessage(contractNegotiationId);
    if (isNull(message)) {
      dataStore.storeConfirmedContract(contractNegotiationId, contractAgreementId);
      return;
    }
    message.getPayload().setContractAgreementId(contractAgreementId);
    initiateDataTransfer(message);
    contractDataStore.add(
        message.getPayload().getAssetId(),
        message.getPayload().getProvider(),
        negotiation.getContractAgreement());
    dataStore.removeMessage(contractNegotiationId);
  }

  private void initiateDataTransfer(Message message) {
    sendInitiationRequest(message);
    message.getPayload().setContractConfirmed(true);
    messageService.send(Channel.DATA_REFERENCE, message);
  }

  private void sendInitiationRequest(Message message) {
    monitor.info(
        String.format(
            "[%s] ContractConfirmationHandler: transfer init - start.", message.getTraceId()));
    DataAddress dataDestination = DataAddress.Builder.newInstance().type("HttpProxy").build();

    TransferType transferType =
        TransferType.Builder.transferType()
            .contentType("application/octet-stream")
            .isFinite(true)
            .build();

    DataRequest dataRequest =
        DataRequest.Builder.newInstance()
            .id(message.getTraceId())
            .assetId(message.getPayload().getAssetId())
            .contractId(message.getPayload().getContractAgreementId())
            .connectorId("provider")
            .connectorAddress(message.getPayload().getProvider())
            .protocol("ids-multipart")
            .dataDestination(dataDestination)
            .managedResources(false)
            .transferType(transferType)
            .build();

    ServiceResult<String> result = transferProcessService.initiateTransfer(dataRequest);
    monitor.info(
        String.format(
            "[%s] ContractConfirmationHandler: transfer init - end", message.getTraceId()));
    if (result.failed()) {
      throw new DataReferenceAccessException(message.getPayload().getAssetId());
    }
  }
}
