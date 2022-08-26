package net.catenax.edc.cp.adapter.process.contractdatastore;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;

public class InMemoryContractDataStore implements ContractDataStore {
  private static final Map<String, ContractAgreementData> contractMap = new HashMap<>();

  @Override
  public void add(String assetId, String provider, ContractAgreement agreement) {
    contractMap.put(getKey(assetId, provider), ContractAgreementData.from(agreement));
  }

  @Override
  public ContractAgreementData get(String assetId, String provider) {
    return contractMap.get(getKey(assetId, provider));
  }

  private String getKey(String assetId, String provider) {
    return assetId + "::" + provider;
  }
}
