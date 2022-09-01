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

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.NoSuchElementException;
import lombok.SneakyThrows;
import net.catenax.edc.data.encryption.util.ArrayUtil;

public class AesInitializationVectorIterator implements Iterator<byte[]> {

  public static final int RANDOM_SIZE = 12;
  public static final int COUNTER_SIZE = 4;
  public static final int VECTOR_SIZE = RANDOM_SIZE + COUNTER_SIZE;

  private final ByteCounter counter;

  public AesInitializationVectorIterator() {
    counter = new ByteCounter(COUNTER_SIZE);
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
    if (counter.isMaxed()) {
      throw new NoSuchElementException(getClass().getSimpleName() + " has no more elements");
    }

    byte[] random = getNextRandom();
    counter.increment();

    return ArrayUtil.concat(random, counter.getBytes());
  }

  @SneakyThrows
  public byte[] getNextRandom() {
    SecureRandom random = SecureRandom.getInstanceStrong();
    byte[] newVector = new byte[RANDOM_SIZE];
    random.nextBytes(newVector);
    return newVector;
  }
}
