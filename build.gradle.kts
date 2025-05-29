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
    `java-test-fixtures`
    alias(libs.plugins.shadow)
    alias(libs.plugins.docker)
    alias(libs.plugins.nexus)
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

    dependencies {

        implementation("org.slf4j:slf4j-api:2.0.17")

        constraints {
            plugins.apply("org.gradle.java-test-fixtures")
            implementation("org.yaml:snakeyaml:2.4") {
                because("version 1.33 has vulnerabilities: https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-1471.")
            }
            implementation("net.minidev:json-smart:2.5.2") {
                because("version 2.4.8 has vulnerabilities: https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-1370.")
            }
            implementation("com.azure:azure-core-http-netty:1.15.11") {
                because("Depends on netty-handler:4.1.115.Final that has a vulnerability: https://ossindex.sonatype.org/component/pkg:maven/io.netty/netty-handler@4.1.115.Final")
            }
            implementation("software.amazon.awssdk:netty-nio-client:2.31.50") {
                because("Depends on netty-handler:4.1.115.Final that has a vulnerability: https://ossindex.sonatype.org/component/pkg:maven/io.netty/netty-handler@4.1.115.Final")
            }
            testImplementation("com.networknt:json-schema-validator:1.5.7") {
                because("There's a conflict between mockserver-netty and identity-hub dependencies for testing, forcing json-schema-validator to 1.5.6 is solving that.")
            }
            testFixturesApi("com.networknt:json-schema-validator:1.5.7") {
                because("There's a conflict between mockserver-netty and identity-hub dependencies for testing, forcing json-schema-validator to 1.5.6 is solving that.")
            }
        }
    }

    configure<org.eclipse.edc.plugins.autodoc.AutodocExtension> {
        processorVersion.set(edcVersion)
        outputDirectory.set(project.layout.buildDirectory.asFile.get())
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

}

subprojects {
    afterEvaluate {
        // the "dockerize" task is added to all projects that use the `shadowJar` plugin
        if (project.plugins.hasPlugin(libs.plugins.shadow.get().pluginId)) {
            val downloadOpentelemetryAgent = tasks.create("downloadOpentelemetryAgent", Copy::class) {
                val openTelemetry = configurations.create("open-telemetry")

                dependencies {
                    openTelemetry(libs.opentelemetry.javaagent)
                }

                from(openTelemetry)
                into("build/resources/otel")
                rename { "opentelemetry-javaagent.jar" }
            }

            val copyLegalDocs = tasks.create("copyLegalDocs", Copy::class) {
                from(project.rootProject.projectDir)
                into("build/legal")
                include("SECURITY.md", "NOTICE.md", "DEPENDENCIES", "LICENSE")
            }

            val copyDockerfile = tasks.create("copyDockerfile", Copy::class) {
                from(rootProject.projectDir.toPath().resolve("resources"))
                into(project.layout.buildDirectory.dir("resources").get().dir("docker"))
                include("Dockerfile")
            }

            val shadowJarTask = tasks.named(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME).get()

            shadowJarTask
                .dependsOn(copyDockerfile)
                .dependsOn(copyLegalDocs)
                .dependsOn(downloadOpentelemetryAgent)

            //actually apply the plugin to the (sub-)project
            apply(plugin = libs.plugins.docker.get().pluginId)

            val dockerTask: DockerBuildImage = tasks.create("dockerize", DockerBuildImage::class) {
                dockerFile.set(File("build/resources/docker/Dockerfile"))

                val dockerContextDir = project.projectDir
                images.add("${project.name}:${project.version}")
                images.add("${project.name}:latest")

                if (System.getProperty("platform") != null) {
                    platform.set(System.getProperty("platform"))
                }

                buildArgs.put("JAR", "build/libs/${project.name}.jar")
                buildArgs.put("OTEL_JAR", "build/resources/otel/opentelemetry-javaagent.jar")
                buildArgs.put("ADDITIONAL_FILES", "build/legal/*")
                inputDir.set(file(dockerContextDir))
            }

            dockerTask.dependsOn(shadowJarTask)
        }

        if (path.startsWith(":edc-tests")) {
            dependencies {
                testImplementation(libs.allure.junit5)
            }

            tasks.withType<Test> {
                useJUnitPlatform()
                systemProperty("allure.results.directory", layout.buildDirectory.dir("allure-results").get().asFile.absolutePath)
            }
        }

        tasks.withType<Test>().configureEach {
            finalizedBy(rootProject.tasks.named("aggregateAllureResults"))
        }
    }

    tasks.register("downloadOpenapi") {
        outputs.dir(project.layout.buildDirectory.dir("docs/openapi"))
        doLast {
            val destinationDirectory = layout.buildDirectory.asFile.get().toPath()
                .resolve("docs").resolve("openapi")

            configurations.asMap.values
                .asSequence()
                .filter { it.isCanBeResolved }
                .map { it.resolvedConfiguration.firstLevelModuleDependencies }.flatten()
                .map { childrenDependencies(it) }.flatten()
                .distinct()
                .forEach { dep ->
                    downloadYamlArtifact(dep, "management-api", destinationDirectory);
                    downloadYamlArtifact(dep, "observability-api", destinationDirectory);
                    downloadYamlArtifact(dep, "public-api", destinationDirectory);
                }
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

tasks.register<Copy>("aggregateAllureResults") {
    group = "reporting"
    description = "Aggregates Allure test results from all subprojects into a single folder"
    doFirst {
        project.delete(layout.buildDirectory.dir("allure-results"))
    }

    subprojects.forEach { subproject ->
        from(subproject.layout.buildDirectory.dir("allure-results"))
    }
    into(layout.buildDirectory.dir("allure-results"))
}


fun childrenDependencies(dependency: ResolvedDependency): List<ResolvedDependency> {
    return listOf(dependency) + dependency.children.map { child -> childrenDependencies(child) }.flatten()
}

fun downloadYamlArtifact(dep: ResolvedDependency, classifier: String, destinationDirectory: java.nio.file.Path) {
    try {
        val managementApi = dependencies.create(dep.moduleGroup, dep.moduleName, dep.moduleVersion, classifier = classifier, ext = "yaml")
        configurations
            .detachedConfiguration(managementApi)
            .resolve()
            .forEach { file ->
                destinationDirectory
                    .resolve("${dep.moduleName}.yaml")
                    .toFile()
                    .let(file::copyTo)
            }
    } catch (_: Exception) {
    }
}
