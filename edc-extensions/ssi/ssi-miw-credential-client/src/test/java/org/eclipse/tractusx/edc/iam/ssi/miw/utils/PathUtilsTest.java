/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.iam.ssi.miw.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class PathUtilsTest {

    private static Stream<Arguments> provideStringsForTrimsPathsCorrect() {
        return Stream.of(
                Arguments.of("http://localhost:8080", "http://localhost:8080"),
                Arguments.of("http://localhost:8080/", "http://localhost:8080"),
                Arguments.of("http://localhost:8080/path", "http://localhost:8080/path"),
                Arguments.of("http://localhost:8080/path/", "http://localhost:8080/path"));
    }

    @ParameterizedTest
    @MethodSource("provideStringsForTrimsPathsCorrect")
    void removeTrailingSlashCorrect(String path, String expected) {
        final String result = PathUtils.removeTrailingSlash(path);

        Assertions.assertEquals(expected, result);
    }
}
