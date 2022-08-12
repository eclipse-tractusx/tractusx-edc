package net.catenax.edc.tests.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransferProcess {
  private String id;
  private String connectorAddress;
  private String contractId;
  private String assetId;
  private String type;
}
