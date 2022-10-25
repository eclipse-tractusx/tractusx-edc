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
package org.eclipse.tractusx.edc.data.encryption.data;

public interface CryptoDataFactory {

  DecryptedData decryptedFromText(String text);

  DecryptedData decryptedFromBase64(String base64);

  DecryptedData decryptedFromBytes(byte[] bytes);

  EncryptedData encryptedFromText(String text);

  EncryptedData encryptedFromBase64(String base64);

  EncryptedData encryptedFromBytes(byte[] bytes);
}
