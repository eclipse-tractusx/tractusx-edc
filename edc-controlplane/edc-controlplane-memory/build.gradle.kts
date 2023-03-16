import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "8.0.0"
}

dependencies {
    implementation(project(":edc-controlplane:edc-controlplane-base"))
    implementation(edc.core.controlplane)
    implementation(edc.azure.vault)
    implementation(edc.azure.identity)

}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}
