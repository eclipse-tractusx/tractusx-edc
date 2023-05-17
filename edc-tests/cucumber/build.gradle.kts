/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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

plugins {
    `java-library`
}

dependencies {
    implementation(project(":edc-extensions:business-partner-validation"))
    implementation(project(":edc-extensions:control-plane-adapter"))
    implementation(project(":edc-extensions:cx-oauth2"))
    implementation(project(":edc-extensions:data-encryption"))
    implementation(project(":edc-extensions:dataplane-selector-configuration"))
    implementation(project(":edc-extensions:hashicorp-vault"))
    implementation(project(":edc-extensions:postgresql-migration"))
    implementation(project(":edc-extensions:provision-additional-headers"))
    implementation(project(":edc-extensions:transferprocess-sftp-client"))
    implementation(project(":edc-extensions:transferprocess-sftp-common"))
    implementation(project(":edc-extensions:transferprocess-sftp-provisioner"))


    testImplementation("com.google.code.gson:gson:2.10.1")
    testImplementation("org.apache.httpcomponents:httpclient:4.5.14")
    testImplementation("org.junit.platform:junit-platform-suite:1.9.3")
    testImplementation("io.cucumber:cucumber-java:7.12.0")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.12.0")
    testImplementation("org.slf4j:slf4j-api:2.0.7")
    testImplementation(libs.restAssured)
    testImplementation(libs.postgres)
    testImplementation(libs.awaitility)
    testImplementation(libs.aws.s3)
    testImplementation(edc.spi.core)
}

tasks.withType(Test::class) {
    onlyIf {
        System.getProperty("cucumber") == "true"
    }
}

// do not publish
edcBuild {
    publish.set(false)
}
