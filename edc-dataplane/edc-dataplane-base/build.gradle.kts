
plugins {
    `java-library`
}

dependencies {
    implementation(edc.config.filesystem)
    implementation(edc.dpf.awss3)
    implementation(edc.dpf.oauth2)
    implementation(edc.dpf.http)

    implementation(edc.dpf.framework)
    implementation(edc.dpf.api)
    implementation(edc.api.observability)
    implementation(edc.core.connector)
    implementation(edc.boot)


    implementation(edc.bundles.monitoring)
    implementation(edc.ext.http)
}