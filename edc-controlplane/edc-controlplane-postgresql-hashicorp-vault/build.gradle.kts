import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    runtimeOnly(project(":edc-controlplane:edc-controlplane-base"))
    runtimeOnly(project(":edc-extensions:postgresql-migration"))
    runtimeOnly(project(":edc-extensions:hashicorp-vault"))
    runtimeOnly(edc.bundles.sqlstores)
    runtimeOnly(edc.transaction.local)
    runtimeOnly(edc.sql.pool)
    runtimeOnly(edc.core.controlplane)
    runtimeOnly(edc.dpf.transfer)

}


tasks.withType<ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
}


application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}
