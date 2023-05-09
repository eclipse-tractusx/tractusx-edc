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
    runtimeOnly(project(":edc-extensions:control-plane-adapter-api"))
    runtimeOnly(project(":edc-extensions:control-plane-adapter-callback"))

    runtimeOnly(libs.edc.core.controlplane)
    runtimeOnly(libs.edc.config.filesystem)
    runtimeOnly(libs.edc.auth.tokenbased)
    runtimeOnly(libs.edc.auth.oauth2.core)
    runtimeOnly(libs.edc.auth.oauth2.daps)
    runtimeOnly(libs.edc.api.management)
    runtimeOnly(libs.edc.ids)
    runtimeOnly(libs.edc.spi.jwt)
    runtimeOnly(libs.bundles.edc.dpf)

    runtimeOnly(libs.edc.ext.http)
    runtimeOnly(libs.bundles.edc.monitoring)
    runtimeOnly(libs.edc.transfer.dynamicreceiver)
    runtimeOnly(libs.edc.controlplane.callback.dispatcher.event)
    runtimeOnly(libs.edc.controlplane.callback.dispatcher.http)

}
