/*
 * Copyright (c) 2023 ZF Friedrichshafen AG
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
package org.eclipse.tractusx.edc.data.encryption.algorithms.aes;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.NoSuchElementException;
import lombok.SneakyThrows;
import org.eclipse.tractusx.edc.data.encryption.util.ArrayUtil;

public class AesInitializationVectorIterator implements Iterator<byte[]> {

  public static final int RANDOM_SIZE = 12;
  public static final int COUNTER_SIZE = 4;

  private final ByteCounter counter;

  private SecureRandom secureRandom;

  public AesInitializationVectorIterator(SecureRandom secureRandom) {
    this.counter = new ByteCounter(COUNTER_SIZE);
    this.secureRandom = secureRandom;
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
    byte[] newVector = new byte[RANDOM_SIZE];
    secureRandom.nextBytes(newVector);
    return newVector;
  }
}
