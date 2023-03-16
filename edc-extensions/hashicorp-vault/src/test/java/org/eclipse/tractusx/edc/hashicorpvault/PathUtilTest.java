/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 * Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package org.eclipse.tractusx.edc.hashicorpvault;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PathUtilTest {

  @ParameterizedTest
  @MethodSource("provideStringsForTrimsPathsCorrect")
  void trimsPathsCorrect(String path, String expected) {
    final String result = PathUtil.trimLeadingOrEndingSlash(path);

    Assertions.assertEquals(expected, result);
  }

  private static Stream<Arguments> provideStringsForTrimsPathsCorrect() {
    return Stream.of(
        Arguments.of("v1/secret/data", "v1/secret/data"),
        Arguments.of("/v1/secret/data", "v1/secret/data"),
        Arguments.of("/v1/secret/data/", "v1/secret/data"),
        Arguments.of("v1/secret/data/", "v1/secret/data"));
  }
}
