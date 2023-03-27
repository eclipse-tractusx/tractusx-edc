/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.tests;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.AbstractResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

@Slf4j
public class BackendServiceBackendAPI {
  private static final String HTTP_HEADER_ACCEPT = "Accept";
  private static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
  private static final String PATH_ROOT = "/";
  private final String backendServiceBackendApiUrl;
  private final HttpClient httpClient;

  public BackendServiceBackendAPI(@NonNull final String backendServiceBackendApiUrl) {
    this.backendServiceBackendApiUrl = backendServiceBackendApiUrl;
    this.httpClient = HttpClientBuilder.create().build();
  }

  /** Lists all files and directories associated by a backend-service path. */
  @SneakyThrows
  public List<String> list(/* @Nullable */ final String path) {
    final URI uri =
        new URIBuilder(backendServiceBackendApiUrl)
            .setPath(Optional.ofNullable(path).orElse(PATH_ROOT))
            .build();
    final HttpGet get = new HttpGet(uri);
    get.setHeader(HTTP_HEADER_ACCEPT, ContentType.APPLICATION_JSON.getMimeType());

    log.debug(String.format("Send %-6s %s", get.getMethod(), get.getURI()));

    return httpClient.execute(get, ListResponseHandler.INSTANCE);
  }

  /** Proves existence of a file or directory associated by a backend-service path. */
  @SneakyThrows
  public boolean exists(@NonNull final String path) {
    final URI uri = new URIBuilder(backendServiceBackendApiUrl).setPath(path).build();
    final HttpHead head = new HttpHead(uri);

    log.debug(String.format("Send %-6s %s", head.getMethod(), head.getURI()));

    return httpClient.execute(head, ExistsResponseHandler.INSTANCE);
  }

  /** Retrieves file content associated by a backend-service path. */
  @SneakyThrows
  public byte[] get(@NonNull final String path) {
    final URI uri = new URIBuilder(backendServiceBackendApiUrl).setPath(path).build();
    final HttpGet get = new HttpGet(uri);
    get.setHeader(HTTP_HEADER_ACCEPT, ContentType.APPLICATION_OCTET_STREAM.getMimeType());

    log.debug(String.format("Send %-6s %s", get.getMethod(), get.getURI()));

    return httpClient.execute(get, GetResponseHandler.INSTANCE);
  }

  /**
   * Creates a file associated by a backend-service path. If existing truncates and recreates that
   * file
   */
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

    log.debug(String.format("Send %-6s %s", post.getMethod(), post.getURI()));

    httpClient.execute(post, PostResponseHandler.INSTANCE);
  }

  @SneakyThrows
  public void post(@NonNull final String path, @NonNull final InputStream inputStream) {
    post(path, inputStream, -1);
  }

  @SneakyThrows
  public void post(@NonNull final String path, @NonNull final byte[] content) {
    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content)) {
      post(path, byteArrayInputStream, content.length);
    }
  }

  /** Deletes files (and directories in a recursive manner) associated by a backend-service path. */
  @SneakyThrows
  public void delete(@NonNull final String path) {
    final URI uri = new URIBuilder(backendServiceBackendApiUrl).setPath(path).build();
    final HttpDelete delete = new HttpDelete(uri);

    httpClient.execute(delete, DeleteResponseHandler.INSTANCE);
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static final class PostResponseHandler implements ResponseHandler<Void> {
    public static final DeleteResponseHandler INSTANCE = new DeleteResponseHandler();

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

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class DeleteResponseHandler implements ResponseHandler<Void> {
    public static final DeleteResponseHandler INSTANCE = new DeleteResponseHandler();

    private static final List<Integer> ACCEPTABLE_STATUS_CODES =
        Arrays.asList(
            HttpStatus.SC_OK,
            HttpStatus.SC_ACCEPTED,
            HttpStatus.SC_NO_CONTENT,
            HttpStatus.SC_NOT_FOUND);

    @Override
    public Void handleResponse(@NonNull final HttpResponse response) throws IOException {
      final StatusLine statusLine = response.getStatusLine();
      final Integer code = statusLine.getStatusCode();

      // not interested into content so throw it away
      Optional.ofNullable(response.getEntity()).ifPresent(EntityUtils::consumeQuietly);

      if (ACCEPTABLE_STATUS_CODES.contains(code)) {
        return null;
      }

      throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
    }
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class GetResponseHandler extends AbstractResponseHandler<byte[]> {
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

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class ExistsResponseHandler implements ResponseHandler<Boolean> {
    public static final ExistsResponseHandler INSTANCE = new ExistsResponseHandler();

    @Override
    public Boolean handleResponse(@NonNull final HttpResponse response)
        throws HttpResponseException {
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

  private static class ListResponseHandler extends GsonResponseHandler<List<String>> {
    public static final ListResponseHandler INSTANCE = new ListResponseHandler();

    private ListResponseHandler() {
      super(new TypeToken<>() {}); // JVM type erasure: Keep generic args!
    }
  }

  @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
  private static class GsonResponseHandler<T> extends AbstractResponseHandler<T> {
    private static final Gson GSON = new Gson();

    @NonNull private final TypeToken<T> typeToken;

    @Override
    public T handleEntity(@NonNull final HttpEntity entity) throws IOException {
      try (final InputStreamReader inputStreamReader = new InputStreamReader(entity.getContent())) {
        return GSON.fromJson(inputStreamReader, typeToken.getType());
      }
    }
  }
}
