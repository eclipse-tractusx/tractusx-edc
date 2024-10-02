plugins {
    `java-library`
    `maven-publish`
    id(libs.plugins.swagger.get().pluginId)
}
dependencies {

    implementation(project(":edc-extensions:agreements:retirement-evaluation-spi"))
    implementation(libs.edc.runtime.metamodel)
    implementation(libs.edc.api.management)

    implementation(libs.jakarta.rsApi)

    testImplementation(testFixtures(libs.edc.core.jersey))
    testImplementation(libs.edc.spi.core)
    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)
    testImplementation(project(":edc-extensions:agreements:retirement-evaluation-spi"))
}

edcBuild {
    swagger {
        apiGroup.set("control-plane")
    }
}