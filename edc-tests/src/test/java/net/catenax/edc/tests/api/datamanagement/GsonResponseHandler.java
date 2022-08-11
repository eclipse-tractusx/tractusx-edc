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

package net.catenax.edc.tests.api.datamanagement;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.impl.client.AbstractResponseHandler;
import org.apache.http.util.EntityUtils;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
class GsonResponseHandler<T> extends AbstractResponseHandler<T> {
  private static final Gson GSON = new GsonBuilder().create();

  @NonNull private final TypeToken<T> typeToken;

  @Override
  public T handleResponse(final HttpResponse response) throws IOException {
    final StatusLine statusLine = response.getStatusLine();

    final HttpEntity entity = response.getEntity();
    if (statusLine.getStatusCode() >= 300) {
      if (log.isDebugEnabled()) {
        try (final InputStream inputStream = entity.getContent()) {
          log.debug("Status: {} {}", statusLine, readFully(inputStream));
        }
      } else {
        EntityUtils.consume(entity);
      }
      throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
    }

    return entity == null ? null : handleEntity(entity);
  }

  @Override
  public T handleEntity(@NonNull final HttpEntity entity) throws IOException {
    final String payload;
    try (final InputStream inputStream = entity.getContent()) {
      payload = readFully(inputStream);
    }

    return GSON.fromJson(payload, typeToken.getType());
  }

  @SneakyThrows
  private String readFully(final InputStream inputStream) {
    int bufferSize = 1024;
    char[] buffer = new char[bufferSize];
    StringBuilder out = new StringBuilder();
    Reader in = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
    for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
      out.append(buffer, 0, numRead);
    }
    return out.toString();
  }
}
