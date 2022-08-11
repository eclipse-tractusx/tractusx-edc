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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PostResponseHandler implements ResponseHandler<Void> {
  public static final PostResponseHandler INSTANCE = new PostResponseHandler();

  private static final List<Integer> ACCEPTABLE_STATUS_CODES =
      Arrays.asList(HttpStatus.SC_OK, HttpStatus.SC_ACCEPTED, HttpStatus.SC_CREATED);

  @Override
  public Void handleResponse(@NonNull final HttpResponse response) throws IOException {
    final StatusLine statusLine = response.getStatusLine();
    final Integer code = statusLine.getStatusCode();
    final HttpEntity entity = response.getEntity();

    // not interested into content so throw it away
    EntityUtils.consume(entity);

    if (ACCEPTABLE_STATUS_CODES.contains(code)) {
      return null;
    }

    throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
  }
}
