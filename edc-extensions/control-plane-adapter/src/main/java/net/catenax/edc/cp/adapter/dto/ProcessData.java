package net.catenax.edc.cp.adapter.dto;

import static java.lang.System.currentTimeMillis;

import jakarta.ws.rs.core.Response;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;

@Getter
@ToString
public class ProcessData {
  private final long timestamp = currentTimeMillis();

  /** request data */
  private final String assetId;
  private final String provider;
  private String contractOfferId;

  /** contract data * */
  @Setter private String contractNegotiationId;
  @Setter private String contractAgreementId;
  @Setter private boolean isContractConfirmed = false;

  /** result/response data * */
  @Setter private EndpointDataReference endpointDataReference;
  @Setter private String errorMessage;
  @Setter private Response.Status errorStatus;

  public ProcessData(String assetId, String provider) {
    this.assetId = assetId;
    this.provider = provider;
  }

  public ProcessData(String assetId, String provider, String contractOfferId) {
    this.assetId = assetId;
    this.provider = provider;
    this.contractOfferId = contractOfferId;
  }
}
