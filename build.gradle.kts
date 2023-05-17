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

import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin

plugins {
    `java-library`
    `maven-publish`
    `jacoco-report-aggregation`
    id("io.freefair.lombok") version "8.0.1"
    id("com.diffplug.spotless") version "6.18.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.bmuschko.docker-remote-api") version "9.3.1"
}

val javaVersion: String by project
val txScmConnection: String by project
val txWebsiteUrl: String by project
val txScmUrl: String by project
val groupId: String by project
val annotationProcessorVersion: String by project
val metaModelVersion: String by project

buildscript {
    repositories {
        mavenLocal()
    }
    dependencies {
        val edcGradlePluginsVersion: String by project
        classpath("org.eclipse.edc.edc-build:org.eclipse.edc.edc-build.gradle.plugin:${edcGradlePluginsVersion}")
    }
}

// include all subprojects in the jacoco report aggregation
project.subprojects.forEach {
    dependencies {
        jacocoAggregation(project(it.path))
    }
}

allprojects {
    apply(plugin = "org.eclipse.edc.edc-build")
    apply(plugin = "io.freefair.lombok")

    repositories {
        mavenCentral()
    }
    dependencies {
        implementation("org.projectlombok:lombok:1.18.26")
        implementation("org.slf4j:slf4j-api:2.0.7")
        // this is used to counter version conflicts between the JUnit version pulled in by the plugin,
        // and the one expected by IntelliJ
        testImplementation(platform("org.junit:junit-bom:5.9.3"))

        constraints {
            implementation("org.yaml:snakeyaml:2.0") {
                because("version 1.33 has vulnerabilities: https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-1471.")
            }
            implementation("net.minidev:json-smart:2.4.10") {
                because("version 2.4.8 has vulnerabilities: https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-1370.")
            }
        }
    }

    // configure which version of the annotation processor to use. defaults to the same version as the plugin
    configure<org.eclipse.edc.plugins.autodoc.AutodocExtension> {
        processorVersion.set(annotationProcessorVersion)
        outputDirectory.set(project.buildDir)
    }

    configure<org.eclipse.edc.plugins.edcbuild.extensions.BuildExtension> {
        versions {
            // override default dependency versions here
            metaModel.set(metaModelVersion)

        }
        val gid = groupId
        pom {
            // this is actually important, so we can publish under the correct GID
            groupId = gid
            projectName.set(project.name)
            description.set("edc :: ${project.name}")
            projectUrl.set(txWebsiteUrl)
            scmConnection.set(txScmConnection)
            scmUrl.set(txScmUrl)
        }
        swagger {
            title.set((project.findProperty("apiTitle") ?: "Tractus-X REST API") as String)
            description =
                (project.findProperty("apiDescription") ?: "Tractus-X REST APIs - merged by OpenApiMerger") as String
            outputFilename.set(project.name)
            outputDirectory.set(file("${rootProject.projectDir.path}/resources/openapi/yaml"))
            resourcePackages = setOf("org.eclipse.tractusx.edc")
        }
        javaLanguageVersion.set(JavaLanguageVersion.of(javaVersion))
    }

    configure<CheckstyleExtension> {
        configFile = rootProject.file("resources/tx-checkstyle-config.xml")
        configDirectory.set(rootProject.file("resources"))

        //checkstyle violations are reported at the WARN level
        this.isShowViolations = System.getProperty("checkstyle.verbose", "false").toBoolean()
    }


    // publishing to OSSRH is handled by the build plugin, but publishing to GH packages
    // must be configured separately
    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/${System.getenv("REPO")}")
                credentials {
                    username = System.getenv("GITHUB_PACKAGE_USERNAME")
                    password = System.getenv("GITHUB_PACKAGE_PASSWORD")
                }
            }
        }
    }

    // EdcRuntimeExtension uses this to determine the runtime classpath of the module to run.
    tasks.register("printClasspath") {
        doLast {
            println(sourceSets["main"].runtimeClasspath.asPath)
        }
    }

}

// the "dockerize" task is added to all projects that use the `shadowJar` plugin
subprojects {
    afterEvaluate {
        if (project.plugins.hasPlugin("com.github.johnrengelman.shadow") &&
            file("${project.projectDir}/src/main/docker/Dockerfile").exists()
        ) {

            //actually apply the plugin to the (sub-)project

            apply(plugin = "com.bmuschko.docker-remote-api")

            // configure the "dockerize" task
            val dockerTask = tasks.create("dockerize", DockerBuildImage::class) {
                dockerFile.set(file("${project.projectDir}/src/main/docker/Dockerfile"))
                images.add("${project.name}:${project.version}")
                images.add("${project.name}:latest")
                // specify platform with the -Dplatform flag:
                if (System.getProperty("platform") != null)
                    platform.set(System.getProperty("platform"))
                buildArgs.put("JAR", "build/libs/${project.name}.jar")
                inputDir.set(file(project.projectDir))
            }

            // make sure "shadowJar" always runs before "dockerize"
            dockerTask.dependsOn(tasks.findByName(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME))
        }
    }
}
