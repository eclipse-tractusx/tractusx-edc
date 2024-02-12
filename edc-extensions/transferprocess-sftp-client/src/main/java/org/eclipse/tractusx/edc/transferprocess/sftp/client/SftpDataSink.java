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

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.util.sink.ParallelSink;

import java.io.IOException;
import java.util.List;

public class SftpDataSink extends ParallelSink {
    private final SftpClientWrapper sftpClientWrapper;

    public SftpDataSink(SftpClientWrapper sftpClientWrapper) {
        this.sftpClientWrapper = sftpClientWrapper;
    }


    @Override
    protected StreamResult<Object> transferParts(List<DataSource.Part> parts) {
        for (DataSource.Part part : parts) {
            try {
                sftpClientWrapper.uploadFile(part.openStream());
            } catch (IOException e) {
                return StreamResult.error(e.getMessage());
            }
        }
        return StreamResult.success();
    }
}
