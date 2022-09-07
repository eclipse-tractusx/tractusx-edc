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
package net.catenax.edc.oauth2.jwk;

import java.net.URI;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.PublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class JwkPublicKeyResolver implements PublicKeyResolver {
  private final Object synchronizationMonitor = new Object();

  @NonNull private final URI jsonWebKeySetUri;

  @NonNull private final OkHttpClient httpClient;

  @NonNull private final TypeManager typeManager;

  @NonNull private final Monitor monitor;

  @NonNull private final List<PublicKeyReader> jsonWebKeyReaders;

  @NonNull private final Duration interval;

  private final Map<String, PublicKey> keys = new HashMap<>();
  private final AtomicReference<ScheduledExecutorService> executorServiceReference =
      new AtomicReference<>();

  public void start() {
    synchronized (synchronizationMonitor) {
      if (executorServiceReference.get() != null) {
        return;
      }

      final Result<Map<String, PublicKey>> result = synchronizeKeys();
      if (result.failed()) {
        throw new EdcException(
            String.format(
                "Could not synchronize keys with identity provider (%s): %s",
                jsonWebKeySetUri, result.getFailureDetail()));
      }

      final ScheduledExecutorService scheduledExecutorService =
          Executors.newSingleThreadScheduledExecutor();
      executorServiceReference.set(scheduledExecutorService);

      scheduledExecutorService.scheduleWithFixedDelay(
          this::synchronizeKeys, interval.getSeconds(), interval.getSeconds(), TimeUnit.SECONDS);
    }
  }

  public void stop() {
    synchronized (synchronizationMonitor) {
      if (executorServiceReference.get() == null) {
        return;
      }

      final ScheduledExecutorService scheduledExecutorService =
          executorServiceReference.getAndSet(null);

      if (scheduledExecutorService.isTerminated()) {
        return;
      }

      scheduledExecutorService.shutdownNow();
    }
  }

  @Override
  public @Nullable PublicKey resolveKey(final String keyId) {
    if (keyId == null) {
      return null;
    }

    synchronized (synchronizationMonitor) {
      return keys.get(keyId);
    }
  }

  protected Result<Map<String, PublicKey>> synchronizeKeys() {
    final Result<Map<String, PublicKey>> fetchedKeys = fetchKeys();

    if (fetchedKeys.succeeded()) {
      synchronized (synchronizationMonitor) {
        keys.clear();
        keys.putAll(fetchedKeys.getContent());
      }
    }

    return fetchedKeys;
  }

  protected Result<Map<String, PublicKey>> fetchKeys() {
    try {
      final HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.get(jsonWebKeySetUri));
      final Request request = new Request.Builder().url(httpUrl).get().build();
      try (final Response response = httpClient.newCall(request).execute()) {
        if (response.code() == 200) {
          final JsonWebKeySet jsonWebKeySet;
          try (final ResponseBody body = response.body()) {

            if (body == null) {
              final String message =
                  String.format(
                      "Unable to refresh identity provider (%s) keys. An empty response was returned.",
                      jsonWebKeySetUri);
              monitor.severe(message);
              return Result.failure(message);
            }

            jsonWebKeySet = typeManager.readValue(body.string(), JsonWebKeySet.class);
          }

          final List<JsonWebKey> nonNullKeys =
              Optional.ofNullable(jsonWebKeySet.getKeys()).orElseGet(Collections::emptyList);
          if (nonNullKeys.isEmpty()) {
            final String message =
                String.format("No keys returned from identity provider (%s).", jsonWebKeySetUri);
            monitor.warning(message);
            return Result.failure(message);
          }

          return Result.success(deserializeKeys(nonNullKeys));
        } else {
          final String message =
              String.format(
                  "Unable to refresh identity provider (%s) keys. Response code was: %s",
                  jsonWebKeySetUri, response.code());
          monitor.severe(message);
          return Result.failure(message);
        }
      }
    } catch (final Exception exception) {
      final String message =
          String.format(
              "Error resolving identity (%s) provider keys: %s",
              jsonWebKeySetUri, exception.getMessage());
      monitor.severe(message, exception);
      return Result.failure(message);
    }
  }

  private Map<String, PublicKey> deserializeKeys(final List<JsonWebKey> jsonWebKeys) {
    final Map<String, PublicKey> keyMap = new HashMap<>();
    for (final JsonWebKey jsonWebKey :
        Optional.ofNullable(jsonWebKeys).orElseGet(Collections::emptyList)) {
      jsonWebKeyReaders.stream()
          .filter(reader -> reader.canRead(jsonWebKey))
          .findFirst()
          .flatMap(reader -> reader.read(jsonWebKey))
          .ifPresent(keyHolder -> keyMap.put(keyHolder.getKeyId(), keyHolder.getPublicKey()));
    }
    return keyMap;
  }
}
