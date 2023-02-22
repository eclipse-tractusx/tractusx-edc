package org.eclipse.tractusx.edc.tests.data;

import lombok.NonNull;
import lombok.Value;

@Value
public class S3DataAddress implements DataAddress {

  @NonNull String bucketName;
  @NonNull String region;
  @NonNull String keyName;
}
