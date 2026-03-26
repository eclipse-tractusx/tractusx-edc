/********************************************************************************
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
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

plugins {
    `java-library`
}

dependencies {
    api(libs.edc.spi.catalog)
    api(libs.edc.spi.core)
    api(libs.dsp.spi)
    api(project(":spi:dsp-spi-08"))
    api(libs.dsp.spi.http)

    implementation(libs.edc.lib.transform)
    implementation(libs.edc.transform.controlplane)

    testImplementation(libs.edc.junit)
}
