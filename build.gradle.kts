/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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

import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin
import java.time.Duration

plugins {
    checkstyle
    `java-library`
    `maven-publish`
    `jacoco-report-aggregation`
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.bmuschko.docker-remote-api") version "9.4.0"
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

val txScmConnection: String by project
val txWebsiteUrl: String by project
val txScmUrl: String by project
val edcVersion = libs.versions.edc

buildscript {
    repositories {
        mavenLocal()
    }
    dependencies {
        classpath(libs.edc.build.plugin)
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

    repositories {
        mavenCentral()
    }
    dependencies {
        implementation("org.slf4j:slf4j-api:2.0.12")
        // this is used to counter version conflicts between the JUnit version pulled in by the plugin,
        // and the one expected by IntelliJ
        testImplementation(platform("org.junit:junit-bom:5.10.2"))

        constraints {
            implementation("org.yaml:snakeyaml:2.2") {
                because("version 1.33 has vulnerabilities: https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-1471.")
            }
            implementation("net.minidev:json-smart:2.5.0") {
                because("version 2.4.8 has vulnerabilities: https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-1370.")
            }
        }
    }

    // configure which version of the annotation processor to use. defaults to the same version as the plugin
    configure<org.eclipse.edc.plugins.autodoc.AutodocExtension> {
        processorVersion.set(edcVersion)
        outputDirectory.set(project.layout.buildDirectory.asFile.get())
        // uncomment the following lines to enable the Autodoc-2-Markdown converter
        // only available with EDC 0.2.1 SNAPSHOT
        // additionalInputDirectory.set(downloadDir.asFile)
        // downloadDirectory.set(downloadDir.asFile)
    }

    configure<org.eclipse.edc.plugins.edcbuild.extensions.BuildExtension> {
        pom {
            // this is actually important, so we can publish under the correct GID
            groupId = project.group.toString()
            projectName.set(project.name)
            description.set("edc :: ${project.name}")
            projectUrl.set(txWebsiteUrl)
            scmConnection.set(txScmConnection)
            scmUrl.set(txScmUrl)
        }
        swagger {
            title.set((project.findProperty("apiTitle") ?: "Tractus-X REST API") as String)
            description =
                (project.findProperty("apiDescription")
                    ?: "Tractus-X REST APIs - merged by OpenApiMerger") as String
            outputFilename.set(project.name)
            outputDirectory.set(file("${rootProject.projectDir.path}/resources/openapi/yaml"))
            resourcePackages = setOf("org.eclipse.tractusx.edc")
        }
    }

    configure<CheckstyleExtension> {
        configFile = rootProject.file("resources/tx-checkstyle-config.xml")
        configDirectory.set(rootProject.file("resources"))

        // gradle checkstyle plugin only includes java src, so we add allSource and .github folder
        tasks.checkstyleMain {
            this.source = project.sourceSets.main.get().allSource.srcDir(".github")
        }

        tasks.checkstyleTest {
            this.source = project.sourceSets.test.get().allSource
        }

        //checkstyle violations are reported at the WARN level
        this.isShowViolations = System.getProperty("checkstyle.verbose", "true").toBoolean()
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

}

// the "dockerize" task is added to all projects that use the `shadowJar` plugin
subprojects {
    afterEvaluate {
        if (project.plugins.hasPlugin("com.github.johnrengelman.shadow") &&
            file("${project.projectDir}/src/main/docker/Dockerfile").exists()
        ) {
            val buildDir = project.layout.buildDirectory.get().asFile

            val agentFile = buildDir.resolve("opentelemetry-javaagent.jar")
            // create task to download the opentelemetry agent
            val openTelemetryAgentUrl = "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.32.0/opentelemetry-javaagent.jar"
            val downloadOtel = tasks.create("downloadOtel") {
                // only execute task if the opentelemetry agent does not exist. invoke the "clean" task to force
                onlyIf {
                    !agentFile.exists()
                }
                // this task could be the first in the graph, so "build/" may not yet exist. Let's be defensive
                doFirst {
                    buildDir.mkdirs()
                }
                // download the jar file
                doLast {
                    val download = { url: String, destFile: File ->
                        ant.invokeMethod(
                            "get",
                            mapOf("src" to url, "dest" to destFile)
                        )
                    }
                    logger.lifecycle("Downloading OpenTelemetry Agent")
                    download(openTelemetryAgentUrl, agentFile)
                }
            }

            // this task copies some legal docs into the build folder, so we can easily copy them into the docker images
            val copyLegalDocs = tasks.create("copyLegalDocs", Copy::class) {
                from(project.rootProject.projectDir)
                into("${buildDir}/legal")
                include("SECURITY.md", "NOTICE.md", "DEPENDENCIES", "LICENSE")
                dependsOn(tasks.named(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME))
            }

            //actually apply the plugin to the (sub-)project
            apply(plugin = "com.bmuschko.docker-remote-api")
            // configure the "dockerize" task
            val dockerTask: DockerBuildImage = tasks.create("dockerize", DockerBuildImage::class) {
                val dockerContextDir = project.projectDir
                dockerFile.set(file("$dockerContextDir/src/main/docker/Dockerfile"))
                images.add("${project.name}:${project.version}")
                images.add("${project.name}:latest")
                // specify platform with the -Dplatform flag:
                if (System.getProperty("platform") != null)
                    platform.set(System.getProperty("platform"))
                buildArgs.put("JAR", "build/libs/${project.name}.jar")
                buildArgs.put("OTEL_JAR", agentFile.relativeTo(dockerContextDir).path)
                buildArgs.put("ADDITIONAL_FILES", "build/legal/*")
                inputDir.set(file(dockerContextDir))
            }
            // make sure  always runs after "dockerize" and after "copyOtel"
            dockerTask
                .dependsOn(tasks.named(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME))
                .dependsOn(downloadOtel)
                .dependsOn(copyLegalDocs)
        }
    }
}

nexusPublishing {
    transitionCheckOptions {
        maxRetries.set(120)
        delayBetween.set(Duration.ofSeconds(10))
    }
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}
