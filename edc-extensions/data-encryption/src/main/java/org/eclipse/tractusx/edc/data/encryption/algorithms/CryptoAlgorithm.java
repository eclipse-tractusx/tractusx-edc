/*
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
package org.eclipse.tractusx.edc.data.encryption.algorithms;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.eclipse.tractusx.edc.data.encryption.data.DecryptedData;
import org.eclipse.tractusx.edc.data.encryption.data.EncryptedData;
import org.eclipse.tractusx.edc.data.encryption.key.CryptoKey;

public interface CryptoAlgorithm<T extends CryptoKey> {
  EncryptedData encrypt(DecryptedData data, T key)
      throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException,
          NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException;

  DecryptedData decrypt(EncryptedData data, T key)
      throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException,
          NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException;
}
