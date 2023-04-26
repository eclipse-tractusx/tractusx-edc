plugins {
    `java-library`
}

dependencies {
    runtimeOnly(project(":edc-extensions:observability-api-customization"))

    runtimeOnly(libs.edc.config.filesystem)
    runtimeOnly(libs.edc.dpf.awss3)
    runtimeOnly(libs.edc.dpf.oauth2)
    runtimeOnly(libs.edc.dpf.http)

    runtimeOnly(libs.edc.dpf.framework)
    runtimeOnly(libs.edc.dpf.api)
    runtimeOnly(libs.edc.core.connector)
    runtimeOnly(libs.edc.boot)

    runtimeOnly(libs.bundles.edc.monitoring)
    runtimeOnly(libs.edc.ext.http)
}
