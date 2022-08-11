/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package net.catenax.edc.tests.api.backendservice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.AbstractResponseHandler;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class GetResponseHandler extends AbstractResponseHandler<byte[]> {
  public static final GetResponseHandler INSTANCE = new GetResponseHandler();

  private static byte[] readAllBytes(@NonNull final InputStream stream) throws IOException {
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    final byte[] data = new byte[16384];

    int i;
    while ((i = stream.read(data, 0, data.length)) != -1) {
      byteArrayOutputStream.write(data, 0, i);
    }

    return byteArrayOutputStream.toByteArray();
  }

  @Override
  public byte[] handleEntity(@NonNull final HttpEntity entity) throws IOException {
    try (final InputStream inputStream = entity.getContent()) {
      return readAllBytes(inputStream);
    }
  }
}
