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

package org.eclipse.tractusx.edc.transferprocess.sftp.client;

import org.apache.sshd.sftp.client.SftpClient;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

public class SftpClientConfig {
    private int bufferSize = 4096;
    private boolean hostVerification = true;
    private Path knownHostFile = Paths.get(System.getenv("HOME"), ".ssh/known_hosts");
    private int connectionTimeoutSeconds = 10;
    private Collection<SftpClient.OpenMode> writeOpenModes =
            List.of(SftpClient.OpenMode.Create, SftpClient.OpenMode.Write);
    private Collection<SftpClient.OpenMode> readOpenModes = List.of(SftpClient.OpenMode.Read);
    private SftpUser sftpUser;
    private SftpLocation sftpLocation;

    public SftpUser getSftpUser() {
        return sftpUser;
    }

    public SftpLocation getSftpLocation() {
        return sftpLocation;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public boolean isHostVerification() {
        return hostVerification;
    }

    public Path getKnownHostFile() {
        return knownHostFile;
    }

    public int getConnectionTimeoutSeconds() {
        return connectionTimeoutSeconds;
    }

    public Collection<SftpClient.OpenMode> getWriteOpenModes() {
        return writeOpenModes;
    }

    public Collection<SftpClient.OpenMode> getReadOpenModes() {
        return readOpenModes;
    }


    public static class Builder {

        private final SftpClientConfig config;

        private Builder() {
            config = new SftpClientConfig();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public static Builder sftpClientConfig() {
            return new Builder();
        }

        public Builder sftpUser(SftpUser sftpUser) {
            this.config.sftpUser = sftpUser;
            return this;
        }

        public Builder sftpLocation(SftpLocation sftpLocation) {
            this.config.sftpLocation = sftpLocation;
            return this;
        }

        public Builder bufferSize(int bufferSize) {
            this.config.bufferSize = bufferSize;
            return this;
        }

        public Builder hostVerification(boolean hostVerification) {
            this.config.hostVerification = hostVerification;
            return this;
        }

        public Builder knownHostFile(Path knownHostFile) {
            this.config.knownHostFile = knownHostFile;
            return this;
        }

        public Builder connectionTimeoutSeconds(int connectionTimeoutSeconds) {
            this.config.connectionTimeoutSeconds = connectionTimeoutSeconds;
            return this;
        }

        public Builder writeOpenModes(Collection<SftpClient.OpenMode> writeOpenModes) {
            this.config.writeOpenModes = writeOpenModes;
            return this;
        }

        public Builder readOpenModes(Collection<SftpClient.OpenMode> readOpenModes) {
            this.config.readOpenModes = readOpenModes;
            return this;
        }

        public SftpClientConfig build() {
            return config;
        }
    }
}
