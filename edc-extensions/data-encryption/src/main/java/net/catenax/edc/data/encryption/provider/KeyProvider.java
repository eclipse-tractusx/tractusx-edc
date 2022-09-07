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
package net.catenax.edc.data.encryption.provider;

import java.util.stream.Stream;
import net.catenax.edc.data.encryption.key.CryptoKey;

public interface KeyProvider<T extends CryptoKey> {
  T getEncryptionKey();

  Stream<T> getDecryptionKeySet();
}
