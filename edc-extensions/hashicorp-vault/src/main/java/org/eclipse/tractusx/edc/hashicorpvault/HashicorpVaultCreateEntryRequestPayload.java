/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.hashicorpvault;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
class HashicorpVaultCreateEntryRequestPayload {

    @JsonProperty("options")
    private Options options;

    @JsonProperty("data")
    private Map<String, String> data;

    private HashicorpVaultCreateEntryRequestPayload() {
    }

    public Options getOptions() {
        return options;
    }

    public Map<String, String> getData() {
        return data;
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Options {
        @JsonProperty("cas")
        private Integer cas;

        public Integer getCas() {
            return cas;
        }
    }

    public static final class Builder {

        private final HashicorpVaultCreateEntryRequestPayload payload;

        private Builder() {
            payload = new HashicorpVaultCreateEntryRequestPayload();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder options(Options options) {
            this.payload.options = options;
            return this;
        }

        public Builder data(Map<String, String> data) {
            this.payload.data = data;
            return this;
        }

        public HashicorpVaultCreateEntryRequestPayload build() {
            return payload;
        }
    }
}
