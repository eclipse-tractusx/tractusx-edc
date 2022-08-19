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
package net.catenax.edc.data.encryption.algorithms.aes;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.NoSuchElementException;
import net.catenax.edc.data.encryption.util.ArrayUtil;

public class AesInitializationVectorIterator implements Iterator<byte[]> {

  public static final int VECTOR_SIZE = 12;
  public static final int NONCE_SIZE = 4;
  public static final int SIZE = VECTOR_SIZE + NONCE_SIZE;

  private final ByteCounter counter;
  private byte[] vector;

  public AesInitializationVectorIterator() {
    counter = new ByteCounter(NONCE_SIZE);
  }

  public AesInitializationVectorIterator(ByteCounter byteCounter) {
    this.counter = byteCounter;
  }

  @Override
  public boolean hasNext() {
    return !counter.isMaxed();
  }

  @Override
  public byte[] next() {
    if (vector == null) {
      throw new IllegalStateException(getClass().getSimpleName() + " has not been initialized");
    }
    if (counter.isMaxed()) {
      throw new NoSuchElementException(getClass().getSimpleName() + " has no more elements");
    }

    counter.increment();
    return ArrayUtil.concat(vector, counter.getBytes());
  }

  public void initialize() throws NoSuchAlgorithmException {
    SecureRandom random = SecureRandom.getInstanceStrong();
    byte[] newVector = new byte[VECTOR_SIZE];
    random.nextBytes(newVector);
    vector = newVector;
  }

  public boolean isInitialized() {
    return vector != null;
  }
}
