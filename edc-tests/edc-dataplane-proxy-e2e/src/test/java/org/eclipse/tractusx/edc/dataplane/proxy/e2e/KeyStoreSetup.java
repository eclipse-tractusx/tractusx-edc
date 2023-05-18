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

package org.eclipse.tractusx.edc.dataplane.proxy.e2e;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;

/**
 * Sets up a test keystore.
 */
public class KeyStoreSetup {

    public static String createKeyStore(String password) {
        try {
            var ks = KeyStore.getInstance(KeyStore.getDefaultType());

            ks.load(null, password.toCharArray());

            var file = File.createTempFile("test", "-keystore.jks");
            try (var fos = new FileOutputStream(file)) {
                ks.store(fos, password.toCharArray());
            }
            file.deleteOnExit();
            return file.getAbsolutePath();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private KeyStoreSetup() {
    }
}
