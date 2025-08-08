/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
 */

package org.eclipse.tractusx.edc.discovery.v4alpha.exceptions;

import org.eclipse.edc.web.spi.exception.EdcApiException;

/**
 * Exception thrown when an unexpected result occurs during connector discovery operations.
 * This exception extends EdcApiException and is used to signal unexpected failures
 * that occur during the discovery process, such as service errors or unexpected response states.
 */
public class UnexpectedResultApiException extends EdcApiException {
    public UnexpectedResultApiException(String message) {
        super(message);
    }

    @Override
    public String getType() {
        return "Unexpected Result";
    }
}
