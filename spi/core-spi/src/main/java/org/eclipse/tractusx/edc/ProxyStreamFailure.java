/**
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
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
 **/

package org.eclipse.tractusx.edc;

import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure;

import java.io.InputStream;
import java.util.List;

public class ProxyStreamFailure extends StreamFailure {
    private final InputStream content;
    private final String mediaType;
    private final String statusCode;

    public ProxyStreamFailure(
            List<String> messages,
            Reason reason,
            InputStream content,
            String mediaType,
            String statusCode) {
        super(messages, reason);
        this.content = content;
        this.statusCode = statusCode;
        this.mediaType = mediaType;
    }

    public InputStream getContent() {
        return content;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getStatusCode() {
        return statusCode;
    }
}
