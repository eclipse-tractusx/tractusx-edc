plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation(project(":edc-dataplane:edc-dataplane-base"))
    implementation(libs.edc.azure.vault)
    constraints {
        implementation("net.minidev:json-smart:2.4.10") {
            because("version 2.4.8 has vulnerabilities: https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-1370.")
        }
    }
    implementation(libs.edc.azure.identity)
    implementation("com.azure:azure-security-keyvault-secrets:4.6.0")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}
