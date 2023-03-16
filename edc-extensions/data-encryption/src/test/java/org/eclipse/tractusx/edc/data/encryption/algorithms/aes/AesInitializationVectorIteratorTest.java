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
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.SneakyThrows;
import org.eclipse.tractusx.edc.data.encryption.util.ArrayUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AesInitializationVectorIteratorTest {

  @Test
  @SneakyThrows
  void testDistinctVectors() {
    final int vectorCount = 100;
    final SecureRandom secureRandom = new SecureRandom();
    AesInitializationVectorIterator iterator = new AesInitializationVectorIterator(secureRandom);

    List<byte[]> vectors = new ArrayList<>();
    for (var i = 0; i < vectorCount; i++) {
      vectors.add(iterator.next());
    }

    long distinctVectors = vectors.stream().map(ArrayUtil::byteArrayToHex).distinct().count();
    Assertions.assertEquals(vectorCount, distinctVectors);
  }

  @Test
  @SneakyThrows
  void testHasNextTrueOnCounterContinuing() {
    ByteCounter counter = Mockito.mock(ByteCounter.class);
    AesInitializationVectorIterator iterator = new AesInitializationVectorIterator(counter);

    Mockito.when(counter.isMaxed()).thenReturn(false);
    Assertions.assertTrue(iterator.hasNext());
  }

  @Test
  @SneakyThrows
  void testHasNextFalseOnCounterEnd() {
    ByteCounter counter = Mockito.mock(ByteCounter.class);
    AesInitializationVectorIterator iterator = new AesInitializationVectorIterator(counter);

    Mockito.when(counter.isMaxed()).thenReturn(true);
    Assertions.assertFalse(iterator.hasNext());
  }

  @Test
  @SneakyThrows
  void testNoSuchElementExceptionOnCounterEnd() {
    ByteCounter counter = Mockito.mock(ByteCounter.class);
    AesInitializationVectorIterator iterator = new AesInitializationVectorIterator(counter);

    Mockito.when(counter.isMaxed()).thenReturn(true);
    Assertions.assertThrows(NoSuchElementException.class, iterator::next);
  }
}
