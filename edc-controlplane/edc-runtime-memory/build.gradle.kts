plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "8.0.0"
}

dependencies {
    runtimeOnly(project(":edc-controlplane:edc-controlplane-base")) {
        exclude(module = "data-encryption")
    }
    runtimeOnly(project(":edc-dataplane:edc-dataplane-base"))
    runtimeOnly(project(":edc-extensions:vault-memory"))
    runtimeOnly(edc.core.controlplane)
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}
