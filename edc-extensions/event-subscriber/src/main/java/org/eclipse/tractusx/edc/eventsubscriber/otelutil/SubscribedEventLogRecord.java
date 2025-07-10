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

import java.time.Instant;
import java.util.List;

/**
 * Class containing log and event record definition.
 */
public record SubscribedEventLogRecord(
        @JsonProperty("timeUnixNano") long timeUnixNano,
        @JsonProperty("observedTimeUnixNano") long observedTimeUnixNano,
        @JsonProperty("severityText") String severityText,
        @JsonProperty("body") StringValue body,
        @JsonProperty("eventName") String eventName,
        @JsonProperty("severityNumber") int severityNumber,
        @JsonProperty("attributes") List<Attribute> attributes,
        @JsonProperty("traceId") String traceId,
        @JsonProperty("spanId") String spanId
) {
    public static SubscribedEventLogRecord of(StringValue body, String eventName) {
        var nowNanos = Instant.now().toEpochMilli() * 1_000_000;
        return new SubscribedEventLogRecord(nowNanos, nowNanos, "INFORMATION", body, eventName, 10, List.of(), null, null);
    }
}
