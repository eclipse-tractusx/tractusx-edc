
plugins {
    `java-library`
}

dependencies {
    implementation(project(":edc-extensions:business-partner-validation"))
    implementation(project(":edc-extensions:dataplane-selector-configuration"))
    implementation(project(":edc-extensions:data-encryption"))
    implementation(project(":edc-extensions:cx-oauth2"))
    implementation(project(":edc-extensions:control-plane-adapter"))
    implementation(project(":edc-extensions:provision-additional-headers"))

    implementation(edc.core.controlplane)
    implementation(edc.config.filesystem)
    implementation(edc.auth.tokenbased)
    implementation(edc.auth.oauth2.core)
    implementation(edc.auth.oauth2.daps)
    implementation(edc.api.management)
    implementation(edc.api.observability)
    implementation(edc.ids)
    implementation(edc.spi.jwt)
    implementation(edc.bundles.dpf)

    implementation(edc.ext.http)
    implementation(edc.bundles.monitoring)
    implementation(edc.transfer.dynamicreceiver)
}
