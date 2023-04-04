/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.tests;


import java.io.InputStream;
import java.util.List;

public interface BackendDataService {
    List<String> list(String path);

    boolean exists(String path);

    byte[] get(String path);

    void post(String path, InputStream inputStream, long length);

    void post(String path, InputStream inputStream);

    void post(String path, byte[] content);

    void delete(String path);
}
