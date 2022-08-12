package net.catenax.edc.tests.api.datamanagement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
public class DataManagementApiTransferProcess {
  private String id;
  private String connectorId = "foo";
  private String connectorAddress;
  private String contractId;
  private String assetId;
  private boolean managedResources;
  private DataDestination dataDestination;

  @Data
  @AllArgsConstructor
  public static class DataDestination {
    @NonNull private String type;
  }
}
