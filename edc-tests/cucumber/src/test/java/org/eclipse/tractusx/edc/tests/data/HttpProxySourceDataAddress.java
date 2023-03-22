package org.eclipse.tractusx.edc.tests.data;

import lombok.NonNull;
import lombok.Value;

@Value
public class HttpProxySourceDataAddress implements DataAddress {
  @NonNull String baseUrl;
  Oauth2Provision oauth2Provision;

  @Value
  public static class Oauth2Provision {
    @NonNull String tokenUrl;
    @NonNull String clientId;
    @NonNull String clientSecret;
    String scope;
  }
}
