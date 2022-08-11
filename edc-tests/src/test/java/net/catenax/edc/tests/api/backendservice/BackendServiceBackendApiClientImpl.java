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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

@Slf4j
@RequiredArgsConstructor
public class BackendServiceBackendApiClientImpl
    implements BackendServiceBackendApiClient, AutoCloseable {
  private static final String HTTP_HEADER_ACCEPT = "Accept";
  private static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
  private static final String PATH_ROOT = "/";

  @NonNull private final String backendServiceBackendApiUrl;

  @Getter(lazy = true, value = AccessLevel.PRIVATE)
  private final CloseableHttpClient httpClient = loadHttpClient();

  @Override
  public void close() throws Exception {
    getHttpClient().close();
  }

  private CloseableHttpClient loadHttpClient() {
    return HttpClientBuilder.create().build();
  }

  @Override
  @SneakyThrows
  public List<String> list(/*@Nullable*/ final String path) {
    final URI uri =
        new URIBuilder(backendServiceBackendApiUrl)
            .setPath(Optional.ofNullable(path).orElse(PATH_ROOT))
            .build();
    final HttpGet get = new HttpGet(uri);
    get.setHeader(HTTP_HEADER_ACCEPT, ContentType.APPLICATION_JSON.getMimeType());

    if (log.isDebugEnabled()) {
      log.debug("Send {} {}", get.getMethod(), get.getURI());
    }

    return getHttpClient().execute(get, ListResponseHandler.INSTANCE);
  }

  @Override
  @SneakyThrows
  public boolean exists(@NonNull final String path) {
    final URI uri = new URIBuilder(backendServiceBackendApiUrl).setPath(path).build();
    final HttpHead head = new HttpHead(uri);

    if (log.isDebugEnabled()) {
      log.debug("Send {} {}", head.getMethod(), head.getURI());
    }

    return getHttpClient().execute(head, ExistsResponseHandler.INSTANCE);
  }

  @Override
  @SneakyThrows
  public byte[] get(@NonNull final String path) {
    final URI uri = new URIBuilder(backendServiceBackendApiUrl).setPath(path).build();
    final HttpGet get = new HttpGet(uri);
    get.setHeader(HTTP_HEADER_ACCEPT, ContentType.APPLICATION_OCTET_STREAM.getMimeType());

    if (log.isDebugEnabled()) {
      log.debug("Send {} {}", get.getMethod(), get.getURI());
    }

    return getHttpClient().execute(get, GetResponseHandler.INSTANCE);
  }

  @Override
  @SneakyThrows
  public void post(
      @NonNull final String path, @NonNull final InputStream inputStream, long length) {
    final URI uri = new URIBuilder(backendServiceBackendApiUrl).setPath(path).build();
    final HttpPost post = new HttpPost(uri);
    post.addHeader(HTTP_HEADER_CONTENT_TYPE, ContentType.APPLICATION_OCTET_STREAM.getMimeType());
    final BasicHttpEntity entity = new BasicHttpEntity();
    entity.setContent(inputStream);
    entity.setContentLength(length);

    post.setEntity(entity);

    if (log.isDebugEnabled()) {
      log.debug("Send {} {}", post.getMethod(), post.getURI());
    }

    getHttpClient().execute(post, PostResponseHandler.INSTANCE);
  }

  @Override
  @SneakyThrows
  public void post(@NonNull final String path, @NonNull final InputStream inputStream) {
    post(path, inputStream, -1);
  }

  @Override
  @SneakyThrows
  public void post(@NonNull final String path, @NonNull final byte[] content) {
    try (final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content)) {
      post(path, byteArrayInputStream, content.length);
    }
  }

  @Override
  @SneakyThrows
  public void delete(@NonNull final String path) {
    final URI uri = new URIBuilder(backendServiceBackendApiUrl).setPath(path).build();
    final HttpDelete delete = new HttpDelete(uri);

    if (log.isDebugEnabled()) {
      log.debug("Send {} {}", delete.getMethod(), delete.getURI());
    }

    getHttpClient().execute(delete, DeleteResponseHandler.INSTANCE);
  }
}
