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

import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class ExistsResponseHandler implements ResponseHandler<Boolean> {
  public static final ExistsResponseHandler INSTANCE = new ExistsResponseHandler();

  @Override
  public Boolean handleResponse(@NonNull final HttpResponse response) throws HttpResponseException {
    final StatusLine statusLine = response.getStatusLine();
    final int code = statusLine.getStatusCode();

    Optional.ofNullable(response.getEntity()).ifPresent(EntityUtils::consumeQuietly);

    switch (code) {
      case HttpStatus.SC_OK:
        return true;
      case HttpStatus.SC_NOT_FOUND:
        return false;
      default:
        throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
    }
  }
}
