/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.helpers;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.io.StringWriter;
import java.security.Key;

public class IatpHelperFunctions {

    /**
     * Returns the Pem representation of a {@link Key}
     *
     * @param key The input key
     * @return The pem encoded key
     */
    public static String toPemEncoded(Key key) {
        var writer = new StringWriter();
        try (var jcaPEMWriter = new JcaPEMWriter(writer)) {
            jcaPEMWriter.writeObject(key);
        } catch (IOException e) {
            throw new EdcException("Unable to convert private in PEM format ", e);
        }
        return writer.toString();
    }

}
