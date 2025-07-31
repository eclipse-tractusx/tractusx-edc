plugins {
    "java-library"
    id(libs.plugins.swagger.get().pluginId)
}

dependencies {

    api(libs.edc.spi.transform)
    api(libs.edc.spi.web)
    api(libs.edc.spi.jsonld)

    implementation(libs.edc.boot)
    implementation(libs.edc.api.management.config)
    implementation(libs.jakarta.rsApi)
}

edcBuild {
    swagger {
        apiGroup.set("control-plane")
    }
}