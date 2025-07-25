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

/**
 * Represents how arguments are matched.
 * <ul>
 *     <li>ClASS: only the type must match, similar to Mockito's {@code isA(SomeType.class}</li>
 *     <li>PARTIAL: only the specified properties must match, disregarding others</li>
 *     <li>PARTIAL: all properties must match, those that are not listed are expected to be null</li>
 * </ul>
 *
 *  @deprecated since 0.11.0
 */
@Deprecated(since = "0.11.0")
public enum MatchType {
    CLASS, PARTIAL, EXACT
}
