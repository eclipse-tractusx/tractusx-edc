import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "8.0.0"
}

dependencies {
    implementation(project(":edc-controlplane:edc-controlplane-base"))
    implementation(project(":edc-extensions:postgresql-migration"))
    implementation(project(":edc-extensions:hashicorp-vault"))
    implementation(edc.bundles.sqlstores)
    implementation(edc.transaction.local)
    implementation(edc.sql.pool)
    implementation(edc.core.controlplane)
    implementation(edc.dpf.transfer)

}


tasks.withType<ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
}


application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}
