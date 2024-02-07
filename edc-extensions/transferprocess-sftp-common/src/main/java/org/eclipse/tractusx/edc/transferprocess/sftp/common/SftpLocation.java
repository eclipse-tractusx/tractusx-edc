/********************************************************************************
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.edc.transferprocess.sftp.common;

import java.util.Objects;

public class SftpLocation {
    private String host;
    private int port;
    private String path;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return String.format("%s:%d/%s", host, port, path);
    }


    public static class Builder {
        private final SftpLocation location;

        private Builder() {
            location = new SftpLocation();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder host(String host) {
            location.host = host;
            return this;
        }

        public Builder port(int port) {
            location.port = port;
            return this;
        }

        public Builder path(String path) {
            location.path = path;
            return this;
        }

        public SftpLocation build() {
            Objects.requireNonNull(location.host, "host");
            Objects.requireNonNull(location.path, "path");
            if (location.port <= 0) {
                throw new IllegalArgumentException("port must be > 0 but was " + location.port);
            }
            return location;
        }
    }
}
