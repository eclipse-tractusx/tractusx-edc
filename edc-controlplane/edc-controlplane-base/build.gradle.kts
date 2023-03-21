
plugins {
    `java-library`
}

dependencies {
    runtimeOnly(project(":edc-extensions:business-partner-validation"))
    runtimeOnly(project(":edc-extensions:dataplane-selector-configuration"))
    runtimeOnly(project(":edc-extensions:data-encryption"))
    runtimeOnly(project(":edc-extensions:cx-oauth2"))
    runtimeOnly(project(":edc-extensions:control-plane-adapter"))
    runtimeOnly(project(":edc-extensions:provision-additional-headers"))
    runtimeOnly(project(":edc-extensions:observability-api-customization"))

    runtimeOnly(edc.core.controlplane)
    runtimeOnly(edc.config.filesystem)
    runtimeOnly(edc.auth.tokenbased)
    runtimeOnly(edc.auth.oauth2.core)
    runtimeOnly(edc.auth.oauth2.daps)
    runtimeOnly(edc.api.management)
    runtimeOnly(edc.ids)
    runtimeOnly(edc.spi.jwt)
    runtimeOnly(edc.bundles.dpf)

    runtimeOnly(edc.ext.http)
    runtimeOnly(edc.bundles.monitoring)
    runtimeOnly(edc.transfer.dynamicreceiver)
}
