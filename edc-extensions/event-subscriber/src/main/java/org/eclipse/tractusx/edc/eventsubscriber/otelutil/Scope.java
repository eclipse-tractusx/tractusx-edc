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
 * Scope of software from which data originates. The instrumentation scope is defined by the (name,version,schema_url,attributes).
 *
 * @param name - name of software
 * @param version - version of software
 * @param attributes - optional list of params
 * */
public record Scope(
        @JsonProperty("name") String name,
        @JsonProperty("version") String version,
        @JsonProperty("attributes") List<Attribute> attributes
) {
}
