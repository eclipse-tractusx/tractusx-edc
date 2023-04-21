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
    runtimeOnly(libs.bundles.edc.sqlstores)
    runtimeOnly(libs.edc.transaction.local)
    runtimeOnly(libs.edc.sql.pool)
    runtimeOnly(libs.edc.core.controlplane)
    runtimeOnly(libs.edc.dpf.transfer)

}


tasks.withType<ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
}


application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}
