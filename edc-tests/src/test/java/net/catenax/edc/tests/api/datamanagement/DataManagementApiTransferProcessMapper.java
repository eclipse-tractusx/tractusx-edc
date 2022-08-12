package net.catenax.edc.tests.api.datamanagement;

import net.catenax.edc.tests.data.TransferProcess;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DataManagementApiTransferProcessMapper {
  DataManagementApiTransferProcessMapper INSTANCE =
      Mappers.getMapper(DataManagementApiTransferProcessMapper.class);

  default DataManagementApiTransferProcess map(final TransferProcess transferProcess) {
    if (transferProcess == null) {
      return null;
    }

    final DataManagementApiTransferProcess dataManagementApiTransferProcess =
        new DataManagementApiTransferProcess();
    dataManagementApiTransferProcess.setAssetId(transferProcess.getAssetId());
    dataManagementApiTransferProcess.setConnectorAddress(transferProcess.getConnectorAddress());
    dataManagementApiTransferProcess.setId(transferProcess.getId());
    dataManagementApiTransferProcess.setContractId(transferProcess.getContractId());
    dataManagementApiTransferProcess.setDataDestination(
        new DataManagementApiTransferProcess.DataDestination("HttpProxy"));
    dataManagementApiTransferProcess.setManagedResources(false);

    return dataManagementApiTransferProcess;
  }

  default TransferProcess map(final DataManagementApiTransferProcess transferProcess) {
    return null;
  }
}
