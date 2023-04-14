
plugins {
    `java-library`
}

dependencies {
    runtimeOnly(project(":edc-extensions:observability-api-customization"))

    runtimeOnly(edc.config.filesystem)
    runtimeOnly(edc.dpf.awss3)
    runtimeOnly(edc.dpf.oauth2)
    runtimeOnly(edc.dpf.http)

    runtimeOnly(edc.dpf.framework)
    runtimeOnly(edc.dpf.api)
    runtimeOnly(edc.core.connector)
    runtimeOnly(edc.boot)

    runtimeOnly(edc.bundles.monitoring)
    runtimeOnly(edc.ext.http)
}