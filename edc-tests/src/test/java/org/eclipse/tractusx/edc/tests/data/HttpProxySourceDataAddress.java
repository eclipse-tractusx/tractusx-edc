package org.eclipse.tractusx.edc.tests.data;

import lombok.NonNull;
import lombok.Value;

@Value
public class HttpProxySourceDataAddress implements DataAddress {
  @NonNull String baseUrl;
}
