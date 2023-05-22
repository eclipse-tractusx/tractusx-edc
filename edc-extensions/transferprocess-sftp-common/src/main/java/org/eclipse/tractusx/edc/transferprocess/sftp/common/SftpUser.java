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

package org.eclipse.tractusx.edc.transferprocess.sftp.common;

import java.security.KeyPair;

public class SftpUser {
    private String name;
    private String password;
    private KeyPair keyPair;

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public static class Builder {
        private final SftpUser user;

        private Builder() {
            user = new SftpUser();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder name(String name) {
            user.name = name;
            return this;
        }

        public Builder password(String password) {
            user.password = password;
            return this;
        }

        public Builder keyPair(KeyPair keyPair) {
            user.keyPair = keyPair;
            return this;
        }

        public SftpUser build() {
            return user;
        }
    }
}
