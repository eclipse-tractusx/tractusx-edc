plugins {
    "java-library"
    id(libs.plugins.swagger.get().pluginId)
}

dependencies {

    api(project(":spi:core-spi"))

    api(libs.edc.spi.transform)
    api(libs.edc.spi.web)
    api(libs.edc.spi.jsonld)

    implementation(libs.edc.lib.validator)
    implementation(libs.edc.boot)
    implementation(libs.edc.api.management.config)
    implementation(libs.jakarta.rsApi)

    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)
    testImplementation(testFixtures(libs.edc.core.jersey))
}

edcBuild {
    swagger {
        apiGroup.set("control-plane")
    }
}