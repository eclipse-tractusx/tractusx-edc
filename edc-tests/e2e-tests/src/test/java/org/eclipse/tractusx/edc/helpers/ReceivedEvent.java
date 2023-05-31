/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public class ReceivedEvent {
    private String type;

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "ReceivedEvent{" +
                "type='" + type + '\'' +
                '}';
    }

    public static class Builder {
        private final ReceivedEvent event;

        private Builder(ReceivedEvent event) {
            this.event = event;
        }

        public static Builder newInstance() {
            return new Builder(new ReceivedEvent());
        }

        public Builder type(String type) {
            this.event.type = type;
            return this;
        }

        public ReceivedEvent build() {
            return event;
        }
    }
}

