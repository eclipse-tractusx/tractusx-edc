/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SubscribedEventLogRecord {

    private long timeUnixNano;
    private long observedTimeUnixNano;
    private String severityText;
    private StringValue body;
    private String eventName;
    private int severityNumber;
    private List<Attribute> attributes = new ArrayList<>();
    private String traceId;
    private String spanId;


    public SubscribedEventLogRecord(StringValue body, String eventName) {
        this.body = body;
        this.eventName = eventName;
        var nowNanos = Instant.now().toEpochMilli() * 1_000_000;
        this.timeUnixNano = nowNanos;
        this.observedTimeUnixNano = nowNanos;
        this.severityText = "Information";
        this.severityNumber = 10;
    }


    public long getTimeUnixNano() {
        return timeUnixNano;
    }

    public void setTimeUnixNano(long timeUnixNano) {
        this.timeUnixNano = timeUnixNano;
    }

    public long getObservedTimeUnixNano() {
        return observedTimeUnixNano;
    }

    public void setObservedTimeUnixNano(long observedTimeUnixNano) {
        this.observedTimeUnixNano = observedTimeUnixNano;
    }

    public String getSeverityText() {
        return severityText;
    }

    public void setSeverityText(String severityText) {
        this.severityText = severityText;
    }


    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public int getSeverityNumber() {
        return severityNumber;
    }

    public void setSeverityNumber(int severityNumber) {
        this.severityNumber = severityNumber;
    }


    public StringValue getBody() {
        return body;
    }

    public void setBody(StringValue body) {
        this.body = body;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSpanId() {
        return spanId;
    }
}
