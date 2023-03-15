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
package org.eclipse.tractusx.edc.data.encryption.util;

public class ArrayUtil {

  private ArrayUtil() {}

  public static byte[] concat(byte[] a, byte[] b) {
    byte[] c = new byte[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }

  public static String byteArrayToHex(byte[] a) {
    StringBuilder sb = new StringBuilder(a.length * 2);
    for (byte b : a) sb.append(String.format("%02x", b));
    return sb.toString();
  }

  public static byte[] subArray(byte[] a, int startIndex, int length) {
    if (startIndex + length > a.length) {
      throw new IllegalArgumentException("Start index + length is greater than array length");
    }

    byte[] b = new byte[length];
    System.arraycopy(a, startIndex, b, 0, length);
    return b;
  }
}
