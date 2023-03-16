import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin

plugins {
    `java-library`
    `maven-publish`
    `jacoco-report-aggregation`
    id("io.freefair.lombok") version "6.6.2"
    id("com.diffplug.spotless") version "6.15.0"
    id("com.github.johnrengelman.shadow") version "8.0.0"
    id("com.bmuschko.docker-remote-api") version "9.2.1"
    id("org.sonarqube") version "4.0.0.2929"
}

val javaVersion: String by project
val scmConnection: String by project
val websiteUrl: String by project
val scmUrl: String by project
val groupId: String by project
val defaultVersion: String by project
val annotationProcessorVersion: String by project
val metaModelVersion: String by project

var actualVersion: String = (project.findProperty("version") ?: defaultVersion) as String
if (actualVersion == "unspecified") {
    actualVersion = defaultVersion
}

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
project.subprojects.forEach{
    dependencies{
        jacocoAggregation(project(it.path))
    }
}

// make sure the test report aggregation is done before reporting to sonar
tasks.sonar {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}

allprojects {
    apply(plugin = "org.eclipse.edc.edc-build")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "org.sonarqube")

    repositories {
        mavenCentral()
    }
    dependencies {
        implementation("org.projectlombok:lombok:1.18.26")
        implementation("org.slf4j:slf4j-api:2.0.5")
    }

    // configure which version of the annotation processor to use. defaults to the same version as the plugin
    configure<org.eclipse.edc.plugins.autodoc.AutodocExtension> {
        processorVersion.set(annotationProcessorVersion)
        outputDirectory.set(project.buildDir)
    }

    configure<org.eclipse.edc.plugins.edcbuild.extensions.BuildExtension> {
        versions {
            // override default dependency versions here
            projectVersion.set(actualVersion)
            metaModel.set(metaModelVersion)

        }
        val gid = groupId;
        pom {
            // this is actually important, so we can publish under the correct GID
            groupId = gid
            projectName.set(project.name)
            description.set("edc :: ${project.name}")
            projectUrl.set(websiteUrl)
            scmConnection.set(scmConnection)
            scmUrl.set(scmUrl)
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
        configFile = rootProject.file("resources/edc-checkstyle-config.xml")
        configDirectory.set(rootProject.file("resources"))
    }


    // this is a temporary workaround until we're fully moved to TractusX:
    // publishing to OSSRH is handled by the build plugin, but publishing to GH packages
    // must be configured separately
    publishing{
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/catenax-ng/product-edc")
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
        if (project.plugins.hasPlugin("com.github.johnrengelman.shadow")) {

            //actually apply the plugin to the (sub-)project

            apply(plugin = "com.bmuschko.docker-remote-api")

            // configure the "dockerize" task
            val dockerTask = tasks.create("dockerize", DockerBuildImage::class) {
                dockerFile.set(file("${project.projectDir}/src/main/docker/Dockerfile"))
                images.add("${project.name}:${project.version}")
                images.add("${project.name}:latest")
                buildArgs.put("JAR", "build/libs/${project.name}.jar")
                inputDir.set(file(project.projectDir))
            }

            // make sure "shadowJar" always runs before "dockerize"
            dockerTask.dependsOn(tasks.findByName(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME))
        }
    }

    sonarqube {
        properties {
            property("sonar.moduleKey", "${project.group}-${project.name}")
        }
    }
}
