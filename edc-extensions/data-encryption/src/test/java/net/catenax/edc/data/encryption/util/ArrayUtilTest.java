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
package net.catenax.edc.data.encryption.util;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

class ArrayUtilTest {

  @ParameterizedTest
  @ArgumentsSource(ArrayArgumentsProvider.class)
  void testConcat(byte[] a, byte[] b, byte[] expected) {
    var result = ArrayUtil.concat(a, b);
    Assertions.assertEquals(ArrayUtil.byteArrayToHex(expected), ArrayUtil.byteArrayToHex(result));
  }

  @Test
  void testSubArray() {
    final byte[] expected = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05};
    final byte[] array = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
    final byte[] subArray = ArrayUtil.subArray(array, 1, 5);

    Assertions.assertEquals(ArrayUtil.byteArrayToHex(expected), ArrayUtil.byteArrayToHex(subArray));
  }

  private static class ArrayArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
      return Stream.of(
          Arguments.of(
              new byte[] {0x00, 0x01, 0x02, 0x03},
              new byte[] {0x04, 0x05, 0x06, 0x07},
              new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07}),
          Arguments.of(
              new byte[] {0x00},
              new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07},
              new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07}),
          Arguments.of(
              new byte[] {},
              new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07},
              new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07}),
          Arguments.of(
              new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07},
              new byte[] {},
              new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07}));
    }
  }
}
