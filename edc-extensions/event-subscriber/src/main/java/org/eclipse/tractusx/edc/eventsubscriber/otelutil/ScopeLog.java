/*
 * Copyright (c) 2025 Schaeffler AG
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

package org.eclipse.tractusx.edc.eventsubscriber.otelutil;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Aggregates source from which information comes {@link Scope} and list of detailed logs definitions - {@link SubscribedEventLogRecord}.
 *
 * @param scope - Origin from which log comes from
 * @param logRecords - Actual log information
 */
public record ScopeLog(
        @JsonProperty("scope") Scope scope,
        @JsonProperty("logRecords") List<SubscribedEventLogRecord> logRecords
) {
}
