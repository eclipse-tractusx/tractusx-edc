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

import java.io.InputStream;
import java.util.List;
import lombok.NonNull;

public interface BackendServiceBackendApiClient {

  /** Lists all files and directories associated by a backend-service path. */
  List<String> list(/*@Nullable*/ String path);

  /** Proves existence of a file or directory associated by a backend-service path. */
  boolean exists(@NonNull String path);

  /** Retrieves file content associated by a backend-service path. */
  byte[] get(@NonNull String path);

  /**
   * Creates a file associated by a backend-service path. If existing truncates and recreates that
   * file
   */
  void post(@NonNull String path, @NonNull InputStream inputStream, long length);

  /**
   * Creates a file associated by a backend-service path. If existing truncates and recreates that
   * file
   */
  void post(@NonNull String path, @NonNull InputStream inputStream);

  /**
   * Creates a file associated by a backend-service path. If existing truncates and recreates that
   * file
   */
  void post(@NonNull String path, @NonNull byte[] content);

  /** Deletes files (and directories in a recursive manner) associated by a backend-service path. */
  void delete(@NonNull String path);
}
