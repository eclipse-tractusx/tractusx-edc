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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStreamReader;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.AbstractResponseHandler;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
class GsonResponseHandler<T> extends AbstractResponseHandler<T> {
  private static final Gson GSON = new Gson();

  @NonNull private final TypeToken<T> typeToken;

  @Override
  public T handleEntity(@NonNull final HttpEntity entity) throws IOException {
    try (final InputStreamReader inputStreamReader = new InputStreamReader(entity.getContent())) {
      return GSON.fromJson(inputStreamReader, typeToken.getType());
    }
  }
}
