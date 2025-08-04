plugins {
    "java-library"
    id(libs.plugins.swagger.get().pluginId)
}

dependencies {

    api(project(":spi:core-spi"))
    api(project(":spi:bdrs-client-spi"))

    api(libs.edc.spi.transform)
    api(libs.edc.spi.web)
    api(libs.edc.spi.jsonld)
    api(libs.edc.spi.controlplane)
    api(libs.edc.spi.protocolversion)
    api(libs.dsp.spi.v08)
    api(libs.dsp.spi.v2025)

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