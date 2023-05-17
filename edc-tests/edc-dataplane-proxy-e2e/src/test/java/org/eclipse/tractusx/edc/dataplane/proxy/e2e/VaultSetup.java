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
import java.io.FileWriter;
import java.io.IOException;

/**
 * Generates a test vault implementation.
 */
public class VaultSetup {
    private static final String DELIMITER = "-----";
    private static final String HEADER = DELIMITER + "BEGIN" + " PUBLIC " + "KEY" + DELIMITER;
    private static final String FOOTER = DELIMITER + "END" + " PUBLIC " + "KEY" + DELIMITER;

    private static final String VAULT_CONTENTS = "tx.dpf.data.proxy.public.key=" + HEADER + "\\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyMkG7DSIhMjFOtqQJsr+\\nKtzfKKGGQ/7mBdjwDCEj0ijKLG/LiEYWsbPA8L/oMAIdR4xpLGaajtz6wj7NbMiA\\nrtHF1HA3mNoeKGix7SfobfQ9J7gJJmSE5DA4BxatL4sPMfoV2SJanJQQjOEAA6/i\\nI+o8SeeBc/2YE55O3yeFjdHK5JIwDi9vIkGnDRBd9poyrHYV+7dcyBB45r6BwvoW\\nG41mezzlKbOl0ZtPW1T9fqp+lOiZWIHMY5ml1daGSbTWwfJxc7XfHHa8KCNQcsPR\\nhWYx6PnxvgqQwYPjvqZF7OYAMUOQX8pg6jfYiU4HgUI1jwwGw3UpJq4b3kzD3u4T\\nDQIDAQAB\\n" + FOOTER + "\n";

    public static String createVaultStore() {
        try {
            var file = File.createTempFile("test", "-vault.properties");
            try (var writer = new FileWriter(file)) {
                writer.write(VAULT_CONTENTS);
            }
            file.deleteOnExit();
            return file.getAbsolutePath();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private VaultSetup() {
    }
}
