package net.catenax.edc.cp.adapter.process.contractnotification;

import lombok.RequiredArgsConstructor;
import net.catenax.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import net.catenax.edc.cp.adapter.exception.ExternalRequestException;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.service.TransferProcessService;
import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;

@RequiredArgsConstructor
public class DataTransferInitializer {
  private final Monitor monitor;
  private final TransferProcessService transferProcessService;

  public void initiate(DataReferenceRetrievalDto dto) {
    monitor.info(
        String.format(
            "[%s] ContractConfirmationHandler: transfer init - start.", dto.getTraceId()));
    DataAddress dataDestination = DataAddress.Builder.newInstance().type("HttpProxy").build();

    TransferType transferType =
        TransferType.Builder.transferType()
            .contentType("application/octet-stream")
            .isFinite(true)
            .build();

    DataRequest dataRequest =
        DataRequest.Builder.newInstance()
            .id(dto.getTraceId())
            .assetId(dto.getPayload().getAssetId())
            .contractId(dto.getPayload().getContractAgreementId())
            .connectorId("provider")
            .connectorAddress(dto.getPayload().getProvider())
            .protocol("ids-multipart")
            .dataDestination(dataDestination)
            .managedResources(false)
            .transferType(transferType)
            .build();

    ServiceResult<String> result = transferProcessService.initiateTransfer(dataRequest);
    monitor.info(
        String.format("[%s] ContractConfirmationHandler: transfer init - end", dto.getTraceId()));
    if (result.failed()) {
      throwDataRefRequestException(dto);
    }
  }

  private void throwDataRefRequestException(DataReferenceRetrievalDto dto) {
    throw new ExternalRequestException(
        String.format(
            "Data reference initial request failed! AssetId: %s", dto.getPayload().getAssetId()));
  }
}
