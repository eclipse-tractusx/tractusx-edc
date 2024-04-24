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

package org.eclipse.tractusx.edc.mock;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = RecordedResponseDeserializer.class)
public final class RecordedRequest<I, O> {
    private final I input;
    private final O output;
    private String description;
    private String name;
    private MatchType inputMatchType;

    private RecordedRequest(I input, O output) {
        this.input = input;
        this.output = output;
    }

    public I getInput() {
        return input;
    }

    public O getOutput() {
        return output;
    }

    public MatchType getInputMatchType() {
        return inputMatchType;
    }

    public static class Builder<I, O> {
        private final RecordedRequest<?, ?> instance;

        public Builder(I input, O output) {
            instance = new RecordedRequest<>(input, output);
        }

        public Builder<I, O> inputMatchType(MatchType input) {
            instance.inputMatchType = input;
            return this;
        }

        public Builder<I, O> name(String name) {
            instance.name = name;
            return this;
        }

        public Builder<I, O> description(String description) {
            instance.description = description;
            return this;
        }

        public RecordedRequest<?, ?> build() {
            return instance;
        }
    }
}
