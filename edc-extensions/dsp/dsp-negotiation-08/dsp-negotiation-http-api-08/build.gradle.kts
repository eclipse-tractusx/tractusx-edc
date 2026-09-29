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
    id(libs.plugins.swagger.get().pluginId)
}

dependencies {
    api(libs.dsp.spi)
    api(project(":spi:dsp-spi-08"))
    api(libs.dsp.spi.http)
    api(libs.edc.spi.core)
    api(libs.edc.spi.web)
    api(libs.edc.spi.controlplane)
    api(libs.edc.ext.jsonld)

    implementation(libs.edc.lib.dsp.negotiation.validation)
    implementation(libs.edc.lib.dsp.negotiation.http.api)
    implementation(libs.edc.lib.jersey.providers)


    implementation(libs.jakarta.rsApi)

    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(libs.edc.core.jersey))
    testImplementation(libs.restAssured)
    testImplementation(testFixtures(libs.edc.lib.dsp.negotiation.http.api))

}

edcBuild {
    swagger {
        apiGroup.set("dsp-api")
    }
}
