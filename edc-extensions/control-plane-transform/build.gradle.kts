/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
 ********************************************************************************/

// Copy of org.eclipse.edc:control-plane-transform, carrying a fix for multi-valued ODRL right operands
// in JsonObjectFromPolicyTransformer. Every other class is verbatim upstream; drop this module once the
// fix is released upstream. Mirrors the temporary transform-lib copy carried in 0.12.1.
plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.spi.jsonld)
    api(libs.edc.spi.participant)
    api(libs.edc.spi.cel)
    api(libs.edc.spi.participant.context.config)
    api(libs.edc.spi.transform)
    api(libs.edc.spi.asset)
    api(libs.edc.spi.policy)
    api(libs.edc.spi.contract)
    api(libs.edc.spi.transfer)
    api(libs.edc.spi.catalog)

    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.lib.json)
    testImplementation(libs.edc.lib.jsonld)
    testImplementation(libs.edc.lib.transform)
}
